/*
 * Copyright (c) Splendid Data Product Development B.V. 2020 - 2022
 *
 * This program is free software: You may redistribute and/or modify under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or (at Client's option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, Client should
 * obtain one via www.gnu.org/licenses/.
 */

package com.splendiddata.pgcode.formatter;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.splendiddata.pgcode.formatter.helper.DefaultConfigCreator;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.*;

/**
 * Takes the provided configuration (if any) and fills in default values where necessary.
 * <p>
 * The class that uses configuration settings from this class can safely assume that no null-values will be returned.
 * That should make life a lot easier in the complex task of properly formating some sql code.
 * </p>
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class FormatConfiguration {
    /**
     * Path to the default config inside the jar
     * <p>
     * The default config xml file is created during the maven build by
     * {@link com.splendiddata.pgcode.formatter.helper.DefaultConfigCreator}
     * </p>
     */
    public static final String DEFAULT_CONFIG_PATH = "/com/splendiddata/pgcode/formatter/DefaultConfig.xml";
    private static final Logger log = LogManager.getLogger(FormatConfiguration.class);
    private static final ObjectFactory factory = new ObjectFactory();

    /**
     * The configuration to be used, with all null-fields filled in with default values
     */
    private Configuration effectiveConfiguration;
    private int standardIndent;

    /*
     * If all groups of spaces are to be replaced with tabs then the tabSplitPattern will be created. It will split up
     * every line in chunks of tab-width characters so that later trailing spaces in each chunk can be replaces with a
     * single tab character
     */
    private Pattern tabSplitPattern;
    private Pattern tabReplacementPattern;;

    /*
     * If only the indent is to be replaced by tabs, then the leadingSpacesPattern will be filled to help replacing
     * leading spaces by tabs
     */
    private Pattern leadingSpacesPattern = tabSplitPattern;

    /**
     * Constructor
     *
     * @param providedConfigPath
     *            Path to the provided config xml file. If null, the default config will be used.
     */
    public FormatConfiguration(Path providedConfigPath) {
        Configuration providedConfig = null;
        /*
         * Load provided config (if any)
         */
        if (providedConfigPath != null) {
            if (validateProvidedConfig(providedConfigPath)) {
                try (InputStream inputStream = Files.newInputStream(providedConfigPath)) {
                    JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName() + ":"
                            + ObjectFactory.class.getPackage().getName(), ObjectFactory.class.getClassLoader());
                    Unmarshaller unmarshaller = context.createUnmarshaller();
                    providedConfig = (Configuration) unmarshaller.unmarshal(inputStream);
                    log.info("FormatConfiguration: config " + providedConfigPath + " is used.");
                } catch (IOException | JAXBException e) {
                    log.error("Error loading configuration file " + providedConfigPath, e);
                }
            }
        }
        effectiveConfiguration = completeConfig(providedConfig);
    }

    /**
     * Constructor
     * 
     * @param configFileContent
     *            The string containing the content of the config xml file. If null, the default config will be used.
     * @param pathName
     */
    public FormatConfiguration(String configFileContent, String pathName) {
        Configuration providedConfig = null;
        /*
         * Use provided config (if not null)
         */
        if (configFileContent != null) {
            if (validateProvidedConfig(configFileContent)) {
                try {
                    JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName() + ":"
                            + ObjectFactory.class.getPackage().getName(), ObjectFactory.class.getClassLoader());
                    Unmarshaller unmarshaller = context.createUnmarshaller();
                    StringReader xmlStringReader = new StringReader(configFileContent);
                    providedConfig = (Configuration) unmarshaller.unmarshal(xmlStringReader);

                    if (pathName != null) {
                        log.trace("FormatConfiguration: configuration from profile is used: " + pathName);
                    } else {
                        log.trace("FormatConfiguration: configuration from user preferences is used.");
                    }
                } catch (JAXBException e) {
                    log.error("Error loading configuration file" + configFileContent, e);
                }
            }
        }
        effectiveConfiguration = completeConfig(providedConfig);
    }

    /**
     * Constructor
     *
     * @param providedConfig
     *            The configuration to work with. If null, the default config will be used
     */
    public FormatConfiguration(Configuration providedConfig) {
        effectiveConfiguration = completeConfig(providedConfig);
    }

    /**
     * Copy constructor
     * <p>
     * The original FormatConfiguration is deep copied. The original is supposed to be valid.
     *
     * @param original
     *            The FormatConfiguration to copy.
     */
    public FormatConfiguration(FormatConfiguration original) {
        effectiveConfiguration = ConfigUtil.copy(original.effectiveConfiguration);
        standardIndent = original.getStandardIndent();
    }

    /**
     * Validates the provided config xml file
     *
     * @param providedConfigPath
     *            The Path of the provided xml file
     * @return boolean true if ok, false when an error has been reported
     */
    private static boolean validateProvidedConfig(Path providedConfigPath) {
        XmlValidationErrorHandler errorHandler = new XmlValidationErrorHandler();
        try {
            Validator validator = constructValidator();
            validator.validate(new StreamSource(providedConfigPath.toFile()));
        } catch (SAXException | IOException e) {
            log.error(e, e);
            errorHandler.setResult(false);
        }
        return errorHandler.isResult();
    }

    /**
     * Validates the provided string containing the content of a config xml
     *
     * @param configFileContent
     *            The string containing the content of a config xml file
     * @return boolean true if ok, false when an error has been reported
     */
    private static boolean validateProvidedConfig(String configFileContent) {
        XmlValidationErrorHandler errorHandler = new XmlValidationErrorHandler();
        try {
            Validator validator = constructValidator();
            StringReader xmlStringReader = new StringReader(configFileContent);
            validator.validate(new StreamSource(xmlStringReader));
        } catch (SAXException | IOException e) {
            log.error(e, e);
            errorHandler.setResult(false);
        }
        return errorHandler.isResult();
    }

    /**
     * Creates a new {@link Validator} for plpgsql_code_formatter-v1_0.xsd
     *
     * @return The created validator
     * @throws SAXException
     *             If a SAX error occurs when trying to create a schema
     */
    private static Validator constructValidator() throws SAXException {
        XmlValidationErrorHandler errorHandler = new XmlValidationErrorHandler();
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(
                FormatConfiguration.class.getClassLoader().getResource("META-INF/pgcode_formatter-v1_0.xsd"));
        Validator validator = schema.newValidator();
        validator.setErrorHandler(errorHandler);
        return validator;
    }

    /**
     * Creates an effective configuration by adding missing fields from the default configuration to the provided
     * config.
     *
     * @param providedConfig
     *            The configuration to work with. If null, the default configuration will be used
     * @return Configuration The effective config, where all missing fields from the providedConfig are filled in with
     *         defaults.
     */
    private Configuration completeConfig(Configuration providedConfig) {
        Configuration defaultConfig = null;

        /*
         * Load default config
         */
        try (InputStream inputStream = FormatConfiguration.class.getModule().getResourceAsStream(DEFAULT_CONFIG_PATH)) {
            JAXBContext context = JAXBContext.newInstance(
                    ObjectFactory.class.getPackage().getName() + ":" + ObjectFactory.class.getPackage().getName(),
                    ObjectFactory.class.getClassLoader());
            Unmarshaller unmarshaller = context.createUnmarshaller();
            defaultConfig = (Configuration) unmarshaller.unmarshal(inputStream);
        } catch (IOException | JAXBException e) {
            log.error("Error loading default configuration file " + DEFAULT_CONFIG_PATH, e);
        } catch (IllegalArgumentException e) {
            // This sometimes happens in JUnit tests started in Eclipse because it cannot find the default config file
            // in its search path.
            log.error("Error loading default configuration file " + DEFAULT_CONFIG_PATH, e);
            defaultConfig = DefaultConfigCreator.getConfiguration();
        }

        if (providedConfig == null) {
            log.info("No config provided, default settings will be used");
            standardIndent = defaultConfig.getIndent().getIndentWidth().intValue();
            return defaultConfig;
        }

        Configuration effectiveConfig = ConfigUtil.copy(providedConfig);

        /*
         * General part
         */
        if (effectiveConfig.getLineWidth() == null) {
            effectiveConfig.setLineWidth(defaultConfig.getLineWidth());
        } else if (effectiveConfig.getLineWidth().getWeight() == null) {
            effectiveConfig.getLineWidth().setWeight(defaultConfig.getLineWidth().getWeight());
        }

        /*
         * tabs
         */
        if (effectiveConfig.getTabs() == null) {
            effectiveConfig.setTabs(defaultConfig.getTabs());
        } else {
            if (effectiveConfig.getTabs().getTabsOrSpaces() == null) {
                effectiveConfig.getTabs().setTabsOrSpaces(defaultConfig.getTabs().getTabsOrSpaces());
            }
            if (effectiveConfig.getTabs().getTabWidth() == null) {
                effectiveConfig.getTabs().setTabWidth(defaultConfig.getTabs().getTabWidth());
            }
        }

        /*
         * indent
         */
        if (effectiveConfig.getIndent() == null) {
            Integer tabWidth = effectiveConfig.getTabs().getTabWidth();
            TabsOrSpacesType tabsOrSpaces = effectiveConfig.getTabs().getTabsOrSpaces();
            IndentType indentType = factory.createIndentType();
            indentType.setTabsOrSpaces(tabsOrSpaces);
            indentType.setIndentWidth(tabWidth);
            // Indent inner function
            indentType.setIndentInnerFunction(Boolean.FALSE);
            effectiveConfig.setIndent(indentType);
        } else {
            if (effectiveConfig.getIndent().getTabsOrSpaces() == null) {
                effectiveConfig.getIndent().setTabsOrSpaces(effectiveConfig.getTabs().getTabsOrSpaces());
            }
            if (effectiveConfig.getIndent().getIndentWidth() == null) {
                effectiveConfig.getIndent().setIndentWidth(effectiveConfig.getTabs().getTabWidth());
            } else {
                TabsOrSpacesType tabsOrSpaces = effectiveConfig.getIndent().getTabsOrSpaces();
                int nSpaces;
                if (tabsOrSpaces == TabsOrSpacesType.TABS) {
                    nSpaces = effectiveConfig.getTabs().getTabWidth().intValue()
                            * effectiveConfig.getIndent().getIndentWidth().intValue();
                } else {
                    nSpaces = effectiveConfig.getIndent().getIndentWidth().intValue();
                }
                effectiveConfig.getIndent().setIndentWidth(Integer.valueOf(nSpaces));
            }

            if (effectiveConfig.getIndent().isIndentInnerFunction() == null) {
                effectiveConfig.getIndent().setIndentInnerFunction(Boolean.FALSE);
            }
        }
        standardIndent = effectiveConfig.getIndent().getIndentWidth().intValue();
        /*
         * query config
         */
        if (effectiveConfig.getQueryConfig() == null) {
            effectiveConfig.setQueryConfig(defaultConfig.getQueryConfig());
        } else {
            if (effectiveConfig.getQueryConfig().isMajorKeywordsOnSeparateLine() == null) {
                effectiveConfig.getQueryConfig()
                        .setMajorKeywordsOnSeparateLine(defaultConfig.getQueryConfig().isMajorKeywordsOnSeparateLine());
            }
            if (effectiveConfig.getQueryConfig().getMaxSingleLineQuery() == null) {
                effectiveConfig.getQueryConfig()
                        .setMaxSingleLineQuery(defaultConfig.getQueryConfig().getMaxSingleLineQuery());
            } else if (effectiveConfig.getQueryConfig().getMaxSingleLineQuery().getWeight() == null) {
                effectiveConfig.getQueryConfig().getMaxSingleLineQuery()
                        .setWeight(defaultConfig.getQueryConfig().getMaxSingleLineQuery().getWeight());
            }
            if (effectiveConfig.getQueryConfig().isIndent() == null) {
                effectiveConfig.getQueryConfig().setIndent(defaultConfig.getQueryConfig().isIndent());
            }
        }

        /*
         * empty line
         */
        if (effectiveConfig.getEmptyLine() == null) {
            effectiveConfig.setEmptyLine(defaultConfig.getEmptyLine());
        }

        /*
         * case operand when operand then ...
         */
        effectiveConfig.setCaseOperand(
                completeCaseType(effectiveConfig.getCaseOperand(), defaultConfig.getCaseOperand(), effectiveConfig));

        /*
         * case when condition then ...
         */
        effectiveConfig.setCaseWhen(
                completeCaseType(effectiveConfig.getCaseWhen(), defaultConfig.getCaseWhen(), effectiveConfig));

        /*
         * commaSeparatedListGrouping
         */
        effectiveConfig.setCommaSeparatedListGrouping(completeCommaSeparatedListGrouping(
                effectiveConfig.getCommaSeparatedListGrouping(), defaultConfig.getCommaSeparatedListGrouping(),
                () -> factory.createCommaSeparatedListGroupingType()));

        /*
         * functionDefinitionArgumentGrouping
         */
        effectiveConfig.setFunctionDefinitionArgumentGrouping(
                completeFunctionDefinitionArgumentGroupingType(providedConfig.getFunctionDefinitionArgumentGrouping(),
                        defaultConfig.getFunctionDefinitionArgumentGrouping(),
                        effectiveConfig.getCommaSeparatedListGrouping()));

        /*
         * table definition
         */
        effectiveConfig.setTableDefinition(completeTableDefinitionType(providedConfig.getTableDefinition(),
                defaultConfig.getTableDefinition(), effectiveConfig.getCommaSeparatedListGrouping()));

        /*
         * From item grouping
         */
        effectiveConfig.setFromItemGrouping(completeFromItemGroupingType(providedConfig.getFromItemGrouping(),
                effectiveConfig.getCommaSeparatedListGrouping(), defaultConfig.getFromItemGrouping()));

        /*
         * Target list grouping
         */
        effectiveConfig.setTargetListGrouping(completeCommaSeparatedListGrouping(
                effectiveConfig.getTargetListGrouping(), effectiveConfig.getCommaSeparatedListGrouping(),
                () -> factory.createCommaSeparatedListGroupingType()));

        /*
         * function argument grouping
         */
        effectiveConfig.setFunctionCallArgumentGrouping(completeCommaSeparatedListGrouping(
                effectiveConfig.getFunctionCallArgumentGrouping(), effectiveConfig.getCommaSeparatedListGrouping(),
                () -> factory.createCommaSeparatedListGroupingType()));

        if (effectiveConfig.getLogicalOperatorsIndent() == null) {
            effectiveConfig.setLogicalOperatorsIndent(defaultConfig.getLogicalOperatorsIndent());
        }

        if (effectiveConfig.getLetterCaseFunctions() == null) {
            effectiveConfig.setLetterCaseFunctions(defaultConfig.getLetterCaseFunctions());
        }

        if (effectiveConfig.getLetterCaseKeywords() == null) {
            effectiveConfig.setLetterCaseKeywords(defaultConfig.getLetterCaseKeywords());
        }

        effectiveConfig.setLanguagePlpgsql(
                completeLanguagePlpgsql(providedConfig.getLanguagePlpgsql(), defaultConfig.getLanguagePlpgsql()));

        return effectiveConfig;
    }

    /**
     * Combines the provided config with the effective config filling in null values with defaultvalues
     *
     * @param providedCaseType
     *            The CaseType from the config file
     * @param defaultCaseType
     *            The default CaseType
     * @param effectiveConfig
     *            To provide a maxLineWidth where applicable
     * @return CaseType The effective CaseType to use
     */
    private static CaseType completeCaseType(CaseType providedCaseType, CaseType defaultCaseType,
            Configuration effectiveConfig) {
        CaseType effectiveCaseType = ConfigUtil.copy(defaultCaseType);
        if (providedCaseType == null) {
            effectiveCaseType.getThenPosition()
                    .setMaxPosition(Integer.valueOf(effectiveConfig.getLineWidth().getValue()));
            return effectiveCaseType;
        }
        if (providedCaseType.getMaxSingleLineClause() == null) {
            effectiveCaseType.setMaxSingleLineClause(defaultCaseType.getMaxSingleLineClause());
        } else {
            effectiveCaseType.setMaxSingleLineClause(providedCaseType.getMaxSingleLineClause());
            if (providedCaseType.getMaxSingleLineClause().getWeight() == null) {
                effectiveCaseType.getMaxSingleLineClause()
                        .setWeight(defaultCaseType.getMaxSingleLineClause().getWeight());
            }
        }
        if (providedCaseType.getWhenPosition() == null) {
            effectiveCaseType.setWhenPosition(defaultCaseType.getWhenPosition());
        } else {
            effectiveCaseType.setWhenPosition(providedCaseType.getWhenPosition());
            if (providedCaseType.getWhenPosition().getWeight() == null) {
                effectiveCaseType.getWhenPosition().setWeight(defaultCaseType.getWhenPosition().getWeight());
            }
        }
        if (providedCaseType.getThenPosition() == null) {
            effectiveCaseType.setThenPosition(defaultCaseType.getThenPosition());
        } else {
            effectiveCaseType.setThenPosition(providedCaseType.getThenPosition());
            if (effectiveCaseType.getThenPosition().getWeight() == null) {
                effectiveCaseType.getThenPosition().setWeight(defaultCaseType.getThenPosition().getWeight());
            }
            if (effectiveCaseType.getThenPosition().getMinPosition() == null) {
                effectiveCaseType.getThenPosition().setMinPosition(defaultCaseType.getThenPosition().getMinPosition());
            }
            if (effectiveCaseType.getThenPosition().getMaxPosition() == null) {
                effectiveCaseType.getThenPosition()
                        .setMaxPosition(Integer.valueOf(effectiveConfig.getLineWidth().getValue()));
            }
            if (effectiveCaseType.getThenPosition().getFallbackPosition() == null) {
                effectiveCaseType.getThenPosition()
                        .setFallbackPosition(defaultCaseType.getThenPosition().getFallbackPosition());
            }
        }
        if (providedCaseType.getElsePosition() == null) {
            effectiveCaseType.setElsePosition(defaultCaseType.getElsePosition());
        } else {
            effectiveCaseType.setElsePosition(providedCaseType.getElsePosition());
        }
        if (providedCaseType.getEndPosition() == null) {
            effectiveCaseType.setEndPosition(defaultCaseType.getEndPosition());
        } else {
            effectiveCaseType.setEndPosition(providedCaseType.getEndPosition());
        }
        return effectiveCaseType;
    }

    /**
     * Combines the provided config(s) to the effective config, which doesn't contain null values
     *
     * @param providedGrouping
     *            The FromItemGroupingType from the provided config file
     * @param effectiveCommaSeparatedListGrouping
     *            The effective CommaSeparatedListGroupingType that will provide the comma position if not provided by
     *            the providedGrouping
     * @param defaultGrouping
     *            FromItemGroupingType from the defined default config file
     * @return FromItemGroupingType from the providedGrouping combined (where necessary) with defaults
     */
    private static FromItemGroupingType completeFromItemGroupingType(FromItemGroupingType providedGrouping,
            CommaSeparatedListGroupingType effectiveCommaSeparatedListGrouping, FromItemGroupingType defaultGrouping) {
        FromItemGroupingType effectiveGrouping = ConfigUtil.copy(defaultGrouping);
        if (providedGrouping == null) {
            return effectiveGrouping;
        }
        effectiveGrouping.setAliasAlignment(completeRelativePositionType(providedGrouping.getAliasAlignment(),
                defaultGrouping.getAliasAlignment()));
        if (providedGrouping.getComma() == null) {
            effectiveGrouping.setComma(effectiveCommaSeparatedListGrouping.getCommaBeforeOrAfter());
        } else {
            effectiveGrouping.setComma(defaultGrouping.getComma());
        }
        if (providedGrouping.getMaxSingleLineLength() != null) {
            effectiveGrouping.setMaxSingleLineLength(completeIntegerValueOption(
                    providedGrouping.getMaxSingleLineLength(), defaultGrouping.getMaxSingleLineLength()));
        }
        if (providedGrouping.isMultilineOpeningParenBeforeArgument() == null) {
            effectiveGrouping.setMultilineOpeningParenBeforeArgument(
                    effectiveCommaSeparatedListGrouping.isMultilineOpeningParenBeforeArgument());
        } else {
            effectiveGrouping
                    .setMultilineOpeningParenBeforeArgument(providedGrouping.isMultilineOpeningParenBeforeArgument());
        }
        if (providedGrouping.isMultilineClosingParenOnNewLine() == null) {
            effectiveGrouping.setMultilineClosingParenOnNewLine(
                    effectiveCommaSeparatedListGrouping.isMultilineClosingParenOnNewLine());
        } else {
            effectiveGrouping.setMultilineClosingParenOnNewLine(providedGrouping.isMultilineClosingParenOnNewLine());
        }
        return effectiveGrouping;
    }

    /**
     * Checks if the configured value is present and complete. If it is, then that will be returned. If it is absent,
     * then the defaultValue will be returned. If the configuratedValue is present but incomplete, then a new
     * IntegerValueOption will be created with all values from the configuredValue that were present combined with
     * values from the defaultValue where absent.
     *
     * @param configuredValue
     *            The value from the actual configuration. May be null
     * @param defaultValue
     *            The value from the default configuration
     * @return IntegerValueOption The configuredValue or the defaultValue or a combination
     */
    private static final IntegerValueOption completeIntegerValueOption(IntegerValueOption configuredValue,
            IntegerValueOption defaultValue) {
        if (configuredValue == null) {
            return defaultValue;
        }
        if (configuredValue.getWeight() != null) {
            return configuredValue;
        }
        IntegerValueOption result = factory.createIntegerValueOption();
        result.setValue(configuredValue.getValue());
        result.setWeight(defaultValue.getWeight());
        return result;
    }

    /**
     * Combines the providedConfig and the defaultConfig into a CommaSeparatedListGroupingType that contains no null
     * values
     *
     * @param <T>
     *            The expected class an be CommaSeparatedListGroupingType or a subtype
     * @param providedConfig
     *            The CommaSeparatedListGroupingType from the config file. May be null.
     * @param defaultConfig
     *            The default CommaSeparatedListGroupingType
     * @param csListGrouptingTypefactory
     *            The object factory method that returns the right subclass of the comma separated list
     * @return CommaSeparatedListGroupingType The defaultConfig if providedConfig was null or a copy of the
     *         providedConfig with all null values filled in
     */
    public static final <T extends CommaSeparatedListGroupingType> T completeCommaSeparatedListGrouping(
            T providedConfig, CommaSeparatedListGroupingType defaultConfig, Supplier<T> csListGrouptingTypefactory) {
        T result = csListGrouptingTypefactory.get();
        if (providedConfig == null) {
            result.setMultilineClosingParenOnNewLine(defaultConfig.isMultilineClosingParenOnNewLine());
            result.setMultilineOpeningParenBeforeArgument(defaultConfig.isMultilineOpeningParenBeforeArgument());
            result.setCommaBeforeOrAfter(defaultConfig.getCommaBeforeOrAfter());
            result.setIndent(defaultConfig.getIndent());
            result.setMaxArgumentsPerGroup(defaultConfig.getMaxArgumentsPerGroup());
            result.setMaxLengthOfGroup(defaultConfig.getMaxLengthOfGroup());
            result.setMaxSingleLineLength(defaultConfig.getMaxSingleLineLength());
        } else {
            if (providedConfig.isMultilineClosingParenOnNewLine() == null) {
                result.setMultilineClosingParenOnNewLine(defaultConfig.isMultilineClosingParenOnNewLine());
            } else {
                result.setMultilineClosingParenOnNewLine(providedConfig.isMultilineClosingParenOnNewLine());
            }
            if (providedConfig.isMultilineOpeningParenBeforeArgument() == null) {
                result.setMultilineOpeningParenBeforeArgument(defaultConfig.isMultilineOpeningParenBeforeArgument());
            } else {
                result.setMultilineOpeningParenBeforeArgument(providedConfig.isMultilineOpeningParenBeforeArgument());
            }
            if (providedConfig.getCommaBeforeOrAfter() == null) {
                result.setCommaBeforeOrAfter(defaultConfig.getCommaBeforeOrAfter());
            } else {
                result.setCommaBeforeOrAfter(providedConfig.getCommaBeforeOrAfter());
            }
            result.setIndent(
                    completeCommaSeparatedListIndentType(providedConfig.getIndent(), defaultConfig.getIndent()));
            result.setMaxArgumentsPerGroup(completeIntegerValueOption(providedConfig.getMaxArgumentsPerGroup(),
                    defaultConfig.getMaxArgumentsPerGroup()));
            result.setMaxLengthOfGroup(completeIntegerValueOption(providedConfig.getMaxLengthOfGroup(),
                    defaultConfig.getMaxLengthOfGroup()));
            result.setMaxSingleLineLength(completeIntegerValueOption(providedConfig.getMaxSingleLineLength(),
                    defaultConfig.getMaxSingleLineLength()));
        }
        return result;
    }

    /**
     * Combines the providedConfig and the defaultConfig into a FunctionArgumentGroupingType that contains no null
     * values
     *
     * @param providedConfig
     *            The FunctionArgumentGroupingType from the config file. May be null.
     * @param defaultConfig
     *            The default FunctionArgumentGroupingType. But default comma separated list values will come from the
     *            defaultCsListConfig.
     * @param defaultCsListConfig
     *            The default CommaSeparatedListGroupingType
     * @return FunctionArgumentGroupingType with all values filled
     */
    private static final FunctionDefinitionArgumentGroupingType completeFunctionDefinitionArgumentGroupingType(
            FunctionDefinitionArgumentGroupingType providedConfig, FunctionDefinitionArgumentGroupingType defaultConfig,
            CommaSeparatedListGroupingType defaultCsListConfig) {
        FunctionDefinitionArgumentGroupingType result = ConfigUtil.copy(defaultConfig);
        if (providedConfig == null) {
            result.setArgumentGrouping(ConfigUtil.copy(defaultCsListConfig));
        } else {
            result.setArgumentGrouping(completeCommaSeparatedListGrouping(providedConfig.getArgumentGrouping(),
                    defaultCsListConfig, () -> factory.createCommaSeparatedListGroupingType()));
            result.setArgumentName(
                    completeRelativePositionType(providedConfig.getArgumentName(), defaultConfig.getArgumentName()));
            result.setDataType(completeRelativePositionType(providedConfig.getDataType(), defaultConfig.getDataType()));
            result.setDefaultValue(
                    completeRelativePositionType(providedConfig.getDefaultValue(), defaultConfig.getDefaultValue()));
            if (providedConfig.getDefaultIndicator() != null) {
                result.setDefaultIndicator(providedConfig.getDefaultIndicator());
            }
        }
        if (!RelativePositionTypeEnum.SUBSEQUENT.equals(result.getArgumentName().getAlignment())
                || !RelativePositionTypeEnum.SUBSEQUENT.equals(result.getDataType().getAlignment())
                || !RelativePositionTypeEnum.SUBSEQUENT.equals(result.getDefaultValue().getAlignment())) {
            IntegerValueOption maxArgsPerGroup = factory.createIntegerValueOption();
            maxArgsPerGroup.setValue(1);
            maxArgsPerGroup.setWeight(Float.MAX_VALUE);
            result.getArgumentGrouping().setMaxArgumentsPerGroup(maxArgsPerGroup);
        }
        return result;
    }

    /**
     * Adds default values to the provided config so no null values exist any more in the resulting RelativePositionType
     *
     * @param providedConfig
     *            The RelativePositionType from the provided config file (may be null)
     * @param defaultConfig
     *            The RelativePositionType from the default config
     * @return RelativePositionType without null values
     */
    private static RelativePositionType completeRelativePositionType(RelativePositionType providedConfig,
            RelativePositionType defaultConfig) {
        if (providedConfig == null) {
            return defaultConfig;
        }
        RelativePositionType result = ConfigUtil.copy(defaultConfig);
        if (providedConfig.getAlignment() != null) {
            result.setAlignment(providedConfig.getAlignment());
        }
        if (providedConfig.getMinPosition() != null) {
            result.setMinPosition(providedConfig.getMinPosition());
        }
        if (providedConfig.getMaxPosition() != null) {
            result.setMaxPosition(providedConfig.getMaxPosition());
        }
        return result;
    }

    /**
     * Combines the providedConfig and the defaultConfig into a TableDefinitionType that contains no null values
     *
     * @param providedConfig
     *            The FunctionArgumentGroupingType from the config file. May be null.
     * @param defaultConfig
     *            The default TableDefinitionType. But some comma separated list values may come from the
     *            defaultCsListConfig.
     * @param defaultCsListConfig
     *            The default CommaSeparatedListGroupingType
     * @return TableDefinitionType with all values filled
     */
    private static final TableDefinitionType completeTableDefinitionType(TableDefinitionType providedConfig,
            TableDefinitionType defaultConfig, CommaSeparatedListGroupingType defaultCsListConfig) {
        TableDefinitionType result = ConfigUtil.copy(defaultConfig);
        if (providedConfig != null) {
            result.setDataType(completeRelativePositionType(providedConfig.getDataType(), defaultConfig.getDataType()));
            result.setColumnContraint(completeColumnConstraintRelativePositionType(providedConfig.getColumnContraint(),
                    defaultConfig.getColumnContraint()));
        }
        CommaSeparatedListGroupingType csListGrouping;
        if (providedConfig == null) {
            csListGrouping = ConfigUtil.copy(defaultConfig.getArgumentGrouping());
            csListGrouping.setCommaBeforeOrAfter(defaultCsListConfig.getCommaBeforeOrAfter());
        } else {
            csListGrouping = completeCommaSeparatedListGrouping(providedConfig.getArgumentGrouping(),
                    defaultConfig.getArgumentGrouping(), () -> factory.createCommaSeparatedListGroupingType());
            if (providedConfig.getArgumentGrouping().getCommaBeforeOrAfter() == null) {
                csListGrouping.setCommaBeforeOrAfter(defaultCsListConfig.getCommaBeforeOrAfter());
            }
        }
        /*
         * If absolute alignment is in effect for the data type or the column constraint then the argument grouping can
         * only be one argument per group
         */
        switch (result.getDataType().getAlignment()) {
        case AT_HORIZONTAL_POSITION:
        case VERTICALLY_ALIGNED:
            csListGrouping.getMaxArgumentsPerGroup().setValue(1);
            csListGrouping.getMaxArgumentsPerGroup().setWeight(Float.MAX_VALUE);
            break;
        case SUBSEQUENT:
        default:
            switch (result.getColumnContraint().getAlignment()) {
            case AT_HORIZONTAL_POSITION:
            case UNDER_DATA_TYPE:
            case VERTICALLY_ALIGNED:
                csListGrouping.getMaxArgumentsPerGroup().setValue(1);
                csListGrouping.getMaxArgumentsPerGroup().setWeight(Float.MAX_VALUE);
                break;
            case SUBSEQUENT:
            default:
                break;

            }
            break;

        }
        result.setArgumentGrouping(csListGrouping);
        return result;
    }

    /**
     * Adds default values to the provided config so no null values exist any more in the resulting
     * ColumnConstraintRelativePositionType
     *
     * @param providedConfig
     *            The ColumnConstraintRelativePositionType from the provided config file (may be null)
     * @param defaultConfig
     *            The ColumnConstraintRelativePositionType from the default config
     * @return ColumnConstraintRelativePositionType without null values
     */
    private static ColumnConstraintRelativePositionType completeColumnConstraintRelativePositionType(
            ColumnConstraintRelativePositionType providedConfig, ColumnConstraintRelativePositionType defaultConfig) {
        if (providedConfig == null) {
            return defaultConfig;
        }
        ColumnConstraintRelativePositionType result = ConfigUtil.copy(defaultConfig);
        if (providedConfig.getAlignment() != null) {
            result.setAlignment(providedConfig.getAlignment());
        }
        if (providedConfig.getMinPosition() != null) {
            result.setMinPosition(providedConfig.getMinPosition());
        }
        if (providedConfig.getMaxPosition() != null) {
            result.setMaxPosition(providedConfig.getMaxPosition());
        }
        return result;
    }

    /**
     * Returns the effective CommaSeparatedListIndentType value
     *
     * @param providedIndent
     *            The CommaSeparatedListIndentType from the provided config. May be null.
     * @param defaultIndent
     *            The CommaSeparatedListIndentType from the default config - so with no null values
     * @return CommaSeparatedListIndentType The profidedIndent, the defaultIndent or a combination
     */
    private static final CommaSeparatedListIndentType completeCommaSeparatedListIndentType(
            CommaSeparatedListIndentType providedIndent, CommaSeparatedListIndentType defaultIndent) {
        if (providedIndent == null) {
            return defaultIndent;
        }
        if (providedIndent.getValue() == null || providedIndent.getWeight() == null) {
            CommaSeparatedListIndentType result = factory.createCommaSeparatedListIndentType();
            if (providedIndent.getValue() == null) {
                result.setValue(defaultIndent.getValue());
            } else {
                result.setValue(providedIndent.getValue());
            }
            if (providedIndent.getWeight() == null) {
                result.setWeight(defaultIndent.getWeight());
            } else {
                result.setWeight(providedIndent.getWeight());
            }
            return result;
        }
        return providedIndent;
    }

    /**
     * Completes the providedSettings to a complete PlpgsqlType
     *
     * @param providedSettings
     *            The PlpgsqlType from the config file if any. May be null
     * @param defaultSettings
     *            The PlpgsqlType that will provide default values
     * @return The combined PlpgsqlType with all fields filled
     */
    private static final PlpgsqlType completeLanguagePlpgsql(PlpgsqlType providedSettings,
            PlpgsqlType defaultSettings) {
        if (providedSettings == null) {
            return defaultSettings;
        }

        PlpgsqlType result = factory.createPlpgsqlType();
        result.setDeclareSection(completePlpgsqlDeclareSectionType(providedSettings.getDeclareSection(),
                defaultSettings.getDeclareSection()));
        result.setCodeSection(
                completePlpgsqlCodeSectionType(providedSettings.getCodeSection(), defaultSettings.getCodeSection()));

        return result;
    }

    /**
     * Completes the providedSetings with the defaultSettings
     *
     * @param providedSettings
     *            The PlpgsqlCodeSectionType from the provided config file. May be null
     * @param defaultSettings
     *            The PlpgsqlCodeSectionType that will provide default settings
     * @return PlpgsqlCodeSectionType without null fields
     */
    private static PlpgsqlCodeSectionType completePlpgsqlCodeSectionType(PlpgsqlCodeSectionType providedSettings,
            PlpgsqlCodeSectionType defaultSettings) {
        if (providedSettings == null) {
            return defaultSettings;
        }

        PlpgsqlCodeSectionType result = factory.createPlpgsqlCodeSectionType();
        if (providedSettings.getIfStatement() == null) {
            result.setIfStatement(defaultSettings.getIfStatement());
        } else {
            PlpgsqlIfStatementType ifStatement = factory.createPlpgsqlIfStatementType();
            result.setIfStatement(ifStatement);
            if (providedSettings.getIfStatement().getThen() == null) {
                ifStatement.setThen(defaultSettings.getIfStatement().getThen());
            } else {
                ifStatement.setThen(providedSettings.getIfStatement().getThen());
            }
            if (providedSettings.getIfStatement().getConditionIndent() == null) {
                ifStatement.setConditionIndent(defaultSettings.getIfStatement().getConditionIndent());
            } else {
                ifStatement.setConditionIndent(providedSettings.getIfStatement().getConditionIndent());
            }
        }

        if (providedSettings.getForStatement() == null) {
            result.setForStatement(defaultSettings.getForStatement());
        } else {
            PlpgsqlForStatementType forStatement = factory.createPlpgsqlForStatementType();
            result.setForStatement(forStatement);
            if (providedSettings.getForStatement().getLoop() == null) {
                forStatement.setLoop(defaultSettings.getForStatement().getLoop());
            } else {
                forStatement.setLoop(providedSettings.getForStatement().getLoop());
            }
        }

        return result;
    }

    /**
     * Completes the providedSettings to a complete PlpgsqlDeclareSectionType
     *
     * @param providedSettings
     *            The PlpgsqlDeclareSectionType from the provided config file (if any). May be null
     * @param defaultSettings
     *            The PlpgsqlDeclareSectionType that will provide default values
     * @return PlpgsqlDeclareSectionType with all fields filled
     */
    private static final PlpgsqlDeclareSectionType completePlpgsqlDeclareSectionType(
            PlpgsqlDeclareSectionType providedSettings, PlpgsqlDeclareSectionType defaultSettings) {
        if (providedSettings == null) {
            return defaultSettings;
        }

        PlpgsqlDeclareSectionType resultDeclareSection = factory.createPlpgsqlDeclareSectionType();
        PlpgsqlDeclareDataTypePosition declaredDataTypePosition = providedSettings.getDataTypePosition();

        /*
         * Data type position
         */
        if (declaredDataTypePosition == null) {
            providedSettings.setDataTypePosition(defaultSettings.getDataTypePosition());
        } else {
            PlpgsqlDeclareDataTypePosition resultDataTypePosition = factory.createPlpgsqlDeclareDataTypePosition();
            resultDeclareSection.setDataTypePosition(resultDataTypePosition);
            if (declaredDataTypePosition.getAlignment() == null) {
                resultDataTypePosition.setAlignment(defaultSettings.getDataTypePosition().getAlignment());
            } else {
                resultDataTypePosition.setAlignment(declaredDataTypePosition.getAlignment());
            }
            switch (resultDataTypePosition.getAlignment()) {
            case VERTICALLY_ALIGNED:
                if (providedSettings.getDataTypePosition().getMinPosition() == null) {
                    resultDataTypePosition.setMinPosition(providedSettings.getDataTypePosition().getMinPosition());
                    if (providedSettings.getDataTypePosition().getMaxPosition() == null) {
                        resultDataTypePosition.setMaxPosition(providedSettings.getDataTypePosition().getMaxPosition());
                    } else {
                        resultDataTypePosition.setMaxPosition(providedSettings.getDataTypePosition().getMaxPosition());
                        if (resultDataTypePosition.getMinPosition().intValue() > resultDataTypePosition.getMaxPosition()
                                .intValue()) {
                            resultDataTypePosition.setMinPosition(resultDataTypePosition.getMaxPosition());
                        }
                    }
                } else {
                    resultDataTypePosition.setMinPosition(providedSettings.getDataTypePosition().getMinPosition());
                    if (providedSettings.getDataTypePosition().getMaxPosition() == null) {
                        resultDataTypePosition.setMaxPosition(providedSettings.getDataTypePosition().getMaxPosition());
                        if (resultDataTypePosition.getMinPosition().intValue() > resultDataTypePosition.getMaxPosition()
                                .intValue()) {
                            resultDataTypePosition.setMaxPosition(resultDataTypePosition.getMinPosition());
                        }
                    } else {
                        resultDataTypePosition.setMaxPosition(providedSettings.getDataTypePosition().getMaxPosition());
                        if (resultDataTypePosition.getMinPosition().intValue() > resultDataTypePosition.getMaxPosition()
                                .intValue()) {
                            resultDataTypePosition.setMaxPosition(resultDataTypePosition.getMinPosition());
                            log.warn(
                                    "configuration.languagePlpgsql.dataTypePosition:maxPosition is supposed to be greater than"
                                            + " or equal to configuration.languagePlpgsql.dataTypePosition:minPosition");
                        }
                    }
                }
                break;
            case AT_HORIZONTAL_POSITION:
                if (providedSettings.getDataTypePosition().getMinPosition() == null) {
                    if (providedSettings.getDataTypePosition().getMaxPosition() == null) {
                        log.warn("With configuration.languagePlpgsql.dataTypePosition:alignment=\"atPosition\","
                                + " at least one of configuration.languagePlpgsql.dataTypePosition:minPosition or"
                                + " configuration.languagePlpgsql.dataTypePosition:maxPosition is required. Assumed "
                                + defaultSettings.getDataTypePosition().getMinPosition());
                        resultDataTypePosition.setMinPosition(defaultSettings.getDataTypePosition().getMinPosition());
                        resultDataTypePosition.setMaxPosition(defaultSettings.getDataTypePosition().getMinPosition());
                    } else {
                        resultDataTypePosition.setMinPosition(providedSettings.getDataTypePosition().getMaxPosition());
                        resultDataTypePosition.setMaxPosition(providedSettings.getDataTypePosition().getMaxPosition());
                    }
                } else {
                    resultDataTypePosition.setMinPosition(providedSettings.getDataTypePosition().getMinPosition());
                    resultDataTypePosition.setMaxPosition(providedSettings.getDataTypePosition().getMinPosition());
                    if (providedSettings.getDataTypePosition().getMaxPosition() != null
                            && !providedSettings.getDataTypePosition().getMaxPosition()
                                    .equals(providedSettings.getDataTypePosition().getMinPosition())) {
                        log.warn("With configuration.languagePlpgsql.dataTypePosition:alignment=\"atPosition\","
                                + " configuration.languagePlpgsql.dataTypePosition:minPosition and"
                                + " configuration.languagePlpgsql.dataTypePosition:maxPosition should be equal. Assumed "
                                + defaultSettings.getDataTypePosition().getMinPosition());
                    }
                }
                break;
            case SUBSEQUENT:
                resultDataTypePosition.setMinPosition(Integer.valueOf(0));
                resultDataTypePosition.setMaxPosition(Integer.valueOf(Integer.MAX_VALUE));
                if (providedSettings.getDataTypePosition().getMinPosition() == null) {
                    if (providedSettings.getDataTypePosition().getMaxPosition() != null) {
                        log.warn("configuration.languagePlpgsql.dataTypePosition:maxPosition"
                                + " has no meaning with configuration.languagePlpgsql.dataTypePosition:alignment=\"subsequent\"");
                    }
                } else {
                    if (providedSettings.getDataTypePosition().getMaxPosition() == null) {
                        log.warn("configuration.languagePlpgsql.dataTypePosition:minPosition"
                                + " has no meaning with configuration.languagePlpgsql.dataTypePosition:alignment=\"subsequent\"");
                    } else {
                        log.warn(
                                "configuration.languagePlpgsql.dataTypePosition:minPosition and configuration.languagePlpgsql.dataTypePosition:maxPosition"
                                        + " have no meaning with configuration.languagePlpgsql.dataTypePosition:alignment=\"subsequent\"");
                    }
                }
                break;
            default:
                assert false : "Unknown " + RelativePositionTypeEnum.class.getName() + " value: "
                        + resultDataTypePosition.getAlignment();
                break;
            }
            if (declaredDataTypePosition.getConstantPosition() == null) {
                resultDataTypePosition.setConstantPosition(defaultSettings.getDataTypePosition().getConstantPosition());
            } else {
                resultDataTypePosition.setConstantPosition(declaredDataTypePosition.getConstantPosition());
            }
        }
        return resultDeclareSection;
    }

    /**
     * @return IntegerValueOption the line width setting
     * @see Configuration#getLineWidth()
     */
    public IntegerValueOption getLineWidth() {
        return effectiveConfiguration.getLineWidth();
    }

    /**
     * @return TabsType tab width and the choice for spaces or tabs
     * @see Configuration#getTabs()
     */
    public TabsType getTabs() {
        return effectiveConfiguration.getTabs();
    }

    /**
     * @return IndentType indent width and the choice for spaces or tabs
     * @see Configuration#getIndent()
     */
    public IndentType getIndent() {
        return effectiveConfiguration.getIndent();
    }

    /**
     * @return LetterCaseType the type of letter case
     * @see Configuration#getLetterCaseFunctions()
     */
    public LetterCaseType getLetterCaseFunctions() {
        return effectiveConfiguration.getLetterCaseFunctions();
    }

    /**
     * @return LetterCaseType the type of letter case
     * @see Configuration#getLetterCaseKeywords()
     */
    public LetterCaseType getLetterCaseKeywords() {
        return effectiveConfiguration.getLetterCaseKeywords();
    }

    /**
     * @return QueryConfigType How should a query be formatted
     * @see Configuration#getQueryConfig()
     */
    public QueryConfigType getQueryConfig() {
        return effectiveConfiguration.getQueryConfig();
    }

    /**
     * @return CommaSeparatedListGroupingType How should the arguments in a comma separated list be formatted
     * @see Configuration#getCommaSeparatedListGrouping()
     */
    public CommaSeparatedListGroupingType getCommaSeparatedListGrouping() {
        return effectiveConfiguration.getCommaSeparatedListGrouping();
    }

    /**
     * @return LogicalOperatorsIndentType How should the logical operators be formatted
     * @see Configuration#getLogicalOperatorsIndent()
     */
    public LogicalOperatorsIndentType getLogicalOperatorsIndent() {
        return effectiveConfiguration.getLogicalOperatorsIndent();
    }

    /**
     * @return CommaSeparatedListGroupingType
     * @see com.splendiddata.pgcode.formatter.configuration.xml.v1_0.Configuration#getFunctionCallArgumentGrouping()
     */
    public CommaSeparatedListGroupingType getFunctionCallArgumentGrouping() {
        return effectiveConfiguration.getFunctionCallArgumentGrouping();
    }

    /**
     * @return FunctionDefinitionArgumentGroupingType
     * @see com.splendiddata.pgcode.formatter.configuration.xml.v1_0.Configuration#getFunctionDefinitionArgumentGrouping()
     */
    public FunctionDefinitionArgumentGroupingType getFunctionDefinitionArgumentGrouping() {
        return effectiveConfiguration.getFunctionDefinitionArgumentGrouping();
    }

    /**
     * @return TableDefinitionType
     * @see com.splendiddata.pgcode.formatter.configuration.xml.v1_0.Configuration#getTableDefinition()
     */
    public TableDefinitionType getTableDefinition() {
        return effectiveConfiguration.getTableDefinition();
    }

    /**
     * @return FromItemGroupingType How should the arguments in a function call be formatted
     * @see Configuration#getFromItemGrouping()
     */
    public FromItemGroupingType getFromItemGrouping() {
        return effectiveConfiguration.getFromItemGrouping();
    }

    /**
     * @return CommaSeparatedListGroupingType
     * @see Configuration#getTargetListGrouping()
     */
    public CommaSeparatedListGroupingType getTargetListGrouping() {
        return effectiveConfiguration.getTargetListGrouping();
    }

    /**
     * @return CaseType how a "case operand when operand then ..." clause should be formatted
     * @see Configuration#getCaseOperand()
     */
    public CaseType getCaseOperand() {
        return effectiveConfiguration.getCaseOperand();
    }

    /**
     * @return CaseType how a "case when condition then ..." clause is to be formatted
     * @see Configuration#getCaseWhen()
     */
    public CaseType getCaseWhen() {
        return effectiveConfiguration.getCaseWhen();
    }

    /**
     * @return EmptyLineOption What to do with empty lines
     * @see Configuration#getEmptyLine()
     */
    public EmptyLineOption getEmptyLine() {
        return effectiveConfiguration.getEmptyLine();
    }

    public boolean isIndentInnerFunction() {
        return effectiveConfiguration.getIndent().isIndentInnerFunction();
    }

    /**
     * @return PlpgsqlType
     * @see Configuration#getLanguagePlpgsql()
     */
    public PlpgsqlType getLanguagePlpgsql() {
        return effectiveConfiguration.getLanguagePlpgsql();
    }

    /**
     * Shortcut to get the standard indent setting
     *
     * @return int The standard indent width in nr of spaces
     */
    public int getStandardIndent() {
        return standardIndent;
    }

    /**
     * When the configuration-&gt;tabs-&gt;tabsOrSpaces setting equals TABS then groups of spaces before a tab position
     * must be replaced by a tab character. The tabSplitPattern splits up a text line into chunks with the width of tab
     * positions, of which trailing spaces can easily be replaced by tabs before reuniting the chunks again to lines
     * (see {@link #getTabReplacementPattern()})
     * <p>
     * This method is intended to be used by
     * {@link com.splendiddata.pgcode.formatter.internal.Util#performTabReplacement(FormatConfiguration, String)}
     *
     * @return Pattern
     */
    public Pattern getTabSplitPattern() {
        if (tabSplitPattern == null) {
            assert TabsOrSpacesType.TABS.equals(getTabs()
                    .getTabsOrSpaces()) : "getTabSplitPattern() must only be invoked if configuration->tabs->tabsOrSpaces equals 'TABS'";
            tabSplitPattern = Pattern.compile("(\n)|([^\n]{1," + getTabs().getTabWidth() + "})");
        }
        return tabSplitPattern;
    }

    /**
     * The tabReplacementPattern can be used to replace trailing spaces. This might be handy when the
     * configuration-&gt;tabs-&gt;tabsOrSpaces setting equals TABS. The idea is to first split every line into chunks of
     * tab-width characters using the pattern from {@link #getTabSplitPattern()} and then use the pattern provided here
     * to replace trailing spaces by tab characters.
     * <p>
     * This method is intended to be used by
     * {@link com.splendiddata.pgcode.formatter.internal.Util#performTabReplacement(FormatConfiguration, String)}
     * 
     * @return Pattern
     */
    public Pattern getTabReplacementPattern() {
        if (tabReplacementPattern == null) {
            assert TabsOrSpacesType.TABS.equals(getTabs()
                    .getTabsOrSpaces()) : "getTabReplacementPattern() must only be invoked if configuration->tabs->tabsOrSpaces equals 'TABS'";
            tabReplacementPattern = Pattern.compile("\\s{2,}$");
        }
        return tabReplacementPattern;
    }

    /**
     * Returns a Pattern that can be used to replace leading spaces with tab characters
     * <p>
     * This method is intended to be used by
     * {@link com.splendiddata.pgcode.formatter.internal.Util#performTabReplacement(FormatConfiguration, String)}
     *
     * @return Pattern
     */
    public Pattern getLeadingSpacesPattern() {
        /*
         * If only the indent is to be replaced by tabs, then the leadingSpacesPattern will be filled to help replacing
         * leading spaces by tabs
         */
        if (leadingSpacesPattern == null) {
            assert !TabsOrSpacesType.TABS.equals(getTabs().getTabsOrSpaces()) && TabsOrSpacesType.TABS.equals(getIndent()
                    .getTabsOrSpaces()) : "getLeadingSpacesPattern() must only be invoked if configuration->tabs->tabsOrSpaces equals 'SPACES' and configuration->indent->tabsOrSpaces equals 'TABS'";
            leadingSpacesPattern = Pattern.compile("\\n([\\s^\\n]+)");
        }
        return leadingSpacesPattern;
    }

    /**
     * Overwrites the commaSeparatedListGrouping with with the groupingConfig.
     * <p>
     * This may help determining if a comma separated list will fit on a single line.
     *
     * @param groupingConfig
     *            The CommaSeparatedListGroupingType that is to be used
     * @return FormatConfiguration this
     */
    public FormatConfiguration setCommaSeparatedListGrouping(CommaSeparatedListGroupingType groupingConfig) {
        effectiveConfiguration.setCommaSeparatedListGrouping(completeCommaSeparatedListGrouping(groupingConfig,
                effectiveConfiguration.getCommaSeparatedListGrouping(),
                () -> factory.createCommaSeparatedListGroupingType()));
        return this;
    }

    private static final class XmlValidationErrorHandler implements ErrorHandler {
        boolean result = true;

        @Override
        public void warning(SAXParseException exception) {
            log.warn(exception);
        }

        @Override
        public void error(SAXParseException exception) {
            result = false;
            log.error(exception);
        }

        @Override
        public void fatalError(SAXParseException exception) {
            result = false;
            log.error(exception);
        }

        /**
         * @return boolean the result
         */
        public boolean isResult() {
            return result;
        }

        /**
         * @param result
         *            the result to set
         */
        public void setResult(boolean result) {
            this.result = result;
        }
    }
}
