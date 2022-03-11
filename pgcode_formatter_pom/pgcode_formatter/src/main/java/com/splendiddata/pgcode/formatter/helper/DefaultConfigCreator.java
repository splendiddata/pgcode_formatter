/*
 * Copyright (c) Splendid Data Product Development B.V. 2020 - 2021
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

package com.splendiddata.pgcode.formatter.helper;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.BeforeOrAfterType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CaseElsePositionOption;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CaseEndPositionOption;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CaseThenFallbackPositionOption;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CaseThenPositionOption;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CaseThenPositionType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CaseType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CaseWhenPositionOption;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CaseWhenPositionType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.ColumnConstraintRelativePositionType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.ColumnConstraintRelativePositionTypeEnum;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CommaSeparatedListGroupingType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CommaSeparatedListIndentOption;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CommaSeparatedListIndentType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.Configuration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.DefaultIndicatorEnum;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.EmptyLineOption;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.FromItemGroupingType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.FunctionDefinitionArgumentGroupingType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.IndentType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.IntegerValueOption;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.LetterCaseType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.LogicalOperatorsIndentType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.ObjectFactory;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.PlpgsqlCodeSectionType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.PlpgsqlConditionEndPositionType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.PlpgsqlDeclareConstantPositionType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.PlpgsqlDeclareDataTypePosition;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.PlpgsqlDeclareSectionType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.PlpgsqlForStatementType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.PlpgsqlIfStatementType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.PlpgsqlType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.QueryConfigType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.RelativePositionType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.RelativePositionTypeEnum;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.TableDefinitionType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.TabsOrSpacesType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.TabsType;

/**
 * Creates a default configuration xml file
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class DefaultConfigCreator {
    private static final Logger log = LogManager.getLogger(DefaultConfigCreator.class);

    /**
     * Creates file target/classes/config/defaultConfig.xml
     *
     * @param args
     *            not relevant
     */
    public static void main(String[] args) {
        Path configFile = Paths.get(getProjectDirectory().toString(),
                "target/classes/" + FormatConfiguration.DEFAULT_CONFIG_PATH);
        try {
            Files.createDirectories(configFile.getParent());

            JAXBContext ctx = JAXBContext.newInstance(
                    ObjectFactory.class.getPackage().getName() + ":" + ObjectFactory.class.getPackage().getName(),
                    ObjectFactory.class.getClassLoader());

            Configuration config = getConfiguration();

            /*
             * Now create the file
             */
            Marshaller marshaller = ctx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            try (OutputStream out = Files.newOutputStream(configFile)) {
                marshaller.marshal(config, out);
            }
        } catch (Exception e) {
            log.error("Error creating config file " + configFile.toAbsolutePath().toString(), e);
        }

        System.out.println(DefaultConfigCreator.class.getName() + " created: " + configFile);
    }

    /**
     * Creates a default configuration with every possible value filled in with a sensible value
     *
     * @return Configuration with no null values
     */
    public static Configuration getConfiguration() {
        ObjectFactory factory = new ObjectFactory();

        /*
         * Create config
         */
        Configuration config = factory.createConfiguration();
        IntegerValueOption integerValue = factory.createIntegerValueOption();
        integerValue.setValue(140);
        integerValue.setWeight(Float.valueOf(10));
        config.setLineWidth(integerValue);
        TabsType tabsType = factory.createTabsType();
        tabsType.setTabsOrSpaces(TabsOrSpacesType.SPACES);
        tabsType.setTabWidth(Integer.valueOf(4));
        config.setTabs(tabsType);
        IndentType indentType = factory.createIndentType();
        indentType.setIndentWidth(Integer.valueOf(4));
        indentType.setTabsOrSpaces(TabsOrSpacesType.SPACES);
        indentType.setIndentInnerFunction(Boolean.TRUE);
        config.setIndent(indentType);
        config.setEmptyLine(EmptyLineOption.PRESERVE_ONE);
        config.setLetterCaseFunctions(LetterCaseType.UNCHANGED);
        config.setLetterCaseKeywords(LetterCaseType.UNCHANGED);

        /*
         * comma separated list configuration
         */
        CommaSeparatedListGroupingType commaSeparatedListGrouping = factory.createCommaSeparatedListGroupingType();
        integerValue = factory.createIntegerValueOption();
        integerValue.setValue(80);
        integerValue.setWeight(Float.valueOf(5));
        commaSeparatedListGrouping.setMaxSingleLineLength(integerValue);
        integerValue = factory.createIntegerValueOption();
        integerValue.setValue(10);
        integerValue.setWeight(Float.valueOf(5));
        commaSeparatedListGrouping.setMaxArgumentsPerGroup(integerValue);
        integerValue = factory.createIntegerValueOption();
        integerValue.setValue(50);
        integerValue.setWeight(Float.valueOf(10));
        commaSeparatedListGrouping.setMaxLengthOfGroup(integerValue);
        CommaSeparatedListIndentType commaSeparatedListIndent = factory.createCommaSeparatedListIndentType();
        commaSeparatedListIndent.setValue(CommaSeparatedListIndentOption.UNDER_FIRST_ARGUMENT);
        commaSeparatedListIndent.setWeight(Float.valueOf(10));
        commaSeparatedListGrouping.setIndent(commaSeparatedListIndent);
        commaSeparatedListGrouping.setMultilineClosingParenOnNewLine(Boolean.FALSE);
        commaSeparatedListGrouping.setMultilineOpeningParenBeforeArgument(Boolean.TRUE);
        commaSeparatedListGrouping.setCommaBeforeOrAfter(BeforeOrAfterType.BEFORE);
        config.setCommaSeparatedListGrouping(commaSeparatedListGrouping);

        /*
         * where clause grouping configuration
         */
        config.setTargetListGrouping(commaSeparatedListGrouping);

        /*
         * function call configuration
         */
        config.setFunctionCallArgumentGrouping(commaSeparatedListGrouping);

        LogicalOperatorsIndentType logicalOperatorsIndent = factory.createLogicalOperatorsIndentType();
        logicalOperatorsIndent.setIndent(CommaSeparatedListIndentOption.UNDER_FIRST_ARGUMENT);
        config.setLogicalOperatorsIndent(logicalOperatorsIndent);

        /*
         * function definition configuration
         */
        FunctionDefinitionArgumentGroupingType functionArgumentGrouping = factory
                .createFunctionDefinitionArgumentGroupingType();
        functionArgumentGrouping.setArgumentGrouping(commaSeparatedListGrouping);
        functionArgumentGrouping.setDefaultIndicator(DefaultIndicatorEnum.AS_IS);

        RelativePositionType relativePositionType = factory.createRelativePositionType();
        relativePositionType.setAlignment(RelativePositionTypeEnum.VERTICALLY_ALIGNED);
        relativePositionType.setMinPosition(Integer.valueOf(0));
        relativePositionType.setMaxPosition(Integer.valueOf("VARIADIC".length() + 1));
        functionArgumentGrouping.setArgumentName(relativePositionType);

        relativePositionType = factory.createRelativePositionType();
        relativePositionType.setAlignment(RelativePositionTypeEnum.VERTICALLY_ALIGNED);
        relativePositionType.setMinPosition(Integer.valueOf(5));
        relativePositionType.setMaxPosition(Integer.valueOf(40));
        functionArgumentGrouping.setDataType(relativePositionType);

        relativePositionType = factory.createRelativePositionType();
        relativePositionType.setAlignment(RelativePositionTypeEnum.VERTICALLY_ALIGNED);
        relativePositionType.setMinPosition(Integer.valueOf(10));
        relativePositionType.setMaxPosition(Integer.valueOf(60));
        functionArgumentGrouping.setDefaultValue(relativePositionType);
        config.setFunctionDefinitionArgumentGrouping(functionArgumentGrouping);

        /*
         * table definition configuration
         */
        TableDefinitionType tableDefinition = factory.createTableDefinitionType();
        commaSeparatedListGrouping = factory.createCommaSeparatedListGroupingType();
        integerValue = factory.createIntegerValueOption();
        integerValue.setValue(1);
        integerValue.setWeight(Float.valueOf(1));
        commaSeparatedListGrouping.setMaxSingleLineLength(integerValue);
        integerValue = factory.createIntegerValueOption();
        integerValue.setValue(1);
        integerValue.setWeight(Float.valueOf(10));
        commaSeparatedListGrouping.setMaxArgumentsPerGroup(integerValue);
        integerValue = factory.createIntegerValueOption();
        integerValue.setValue(1);
        integerValue.setWeight(Float.valueOf(1));
        commaSeparatedListGrouping.setMaxLengthOfGroup(integerValue);
        commaSeparatedListIndent = factory.createCommaSeparatedListIndentType();
        commaSeparatedListIndent.setValue(CommaSeparatedListIndentOption.DOUBLE_INDENTED);
        commaSeparatedListIndent.setWeight(Float.valueOf(10));
        commaSeparatedListGrouping.setIndent(commaSeparatedListIndent);
        commaSeparatedListGrouping.setMultilineClosingParenOnNewLine(Boolean.FALSE);
        commaSeparatedListGrouping.setMultilineOpeningParenBeforeArgument(Boolean.TRUE);
        commaSeparatedListGrouping.setCommaBeforeOrAfter(BeforeOrAfterType.BEFORE);
        tableDefinition.setArgumentGrouping(commaSeparatedListGrouping);

        relativePositionType = factory.createRelativePositionType();
        relativePositionType.setAlignment(RelativePositionTypeEnum.VERTICALLY_ALIGNED);
        relativePositionType.setMinPosition(Integer.valueOf(5));
        relativePositionType.setMaxPosition(Integer.valueOf(40));
        tableDefinition.setDataType(relativePositionType);

        ColumnConstraintRelativePositionType relativeColumnConstraintPositionType = factory
                .createColumnConstraintRelativePositionType();
        relativeColumnConstraintPositionType.setAlignment(ColumnConstraintRelativePositionTypeEnum.VERTICALLY_ALIGNED);
        relativeColumnConstraintPositionType.setMinPosition(Integer.valueOf(10));
        relativeColumnConstraintPositionType.setMaxPosition(Integer.valueOf(60));
        tableDefinition.setColumnContraint(relativeColumnConstraintPositionType);
        config.setTableDefinition(tableDefinition);

        /*
         * From item grouping
         */
        FromItemGroupingType fromItemGrouping = factory.createFromItemGroupingType();
        config.setFromItemGrouping(fromItemGrouping);
        fromItemGrouping.setComma(BeforeOrAfterType.BEFORE);
        integerValue = factory.createIntegerValueOption();
        integerValue.setValue(100);
        integerValue.setWeight(Float.valueOf(10));
        fromItemGrouping.setMaxSingleLineLength(integerValue);
        relativePositionType = factory.createRelativePositionType();
        relativePositionType.setAlignment(RelativePositionTypeEnum.VERTICALLY_ALIGNED);
        relativePositionType.setMinPosition(Integer.valueOf(10));
        relativePositionType.setMaxPosition(Integer.valueOf(60));
        fromItemGrouping.setAliasAlignment(relativePositionType);
        fromItemGrouping.setMultilineOpeningParenBeforeArgument(Boolean.TRUE);
        fromItemGrouping.setMultilineClosingParenOnNewLine(Boolean.FALSE);

        /*
         * Query configuration
         */
        QueryConfigType queryConfig = factory.createQueryConfigType();
        integerValue = factory.createIntegerValueOption();
        integerValue.setValue(60);
        integerValue.setWeight(Float.valueOf(5));
        queryConfig.setMaxSingleLineQuery(integerValue);
        queryConfig.setMajorKeywordsOnSeparateLine(Boolean.FALSE);
        queryConfig.setIndent(Boolean.FALSE);
        config.setQueryConfig(queryConfig);

        /*
         * case operand when operand then ...
         */
        CaseType caseType = factory.createCaseType();
        integerValue = factory.createIntegerValueOption();
        integerValue.setValue(60);
        integerValue.setWeight(Float.valueOf(5));
        caseType.setMaxSingleLineClause(integerValue);
        CaseWhenPositionType whenPosition = factory.createCaseWhenPositionType();
        whenPosition.setValue(CaseWhenPositionOption.WHEN_UNDER_CASE);
        whenPosition.setWeight(Float.valueOf(10));
        caseType.setWhenPosition(whenPosition);
        CaseThenPositionType thenPosition = factory.createCaseThenPositionType();
        thenPosition.setValue(CaseThenPositionOption.THEN_AFTER_WHEN_ALIGNED);
        thenPosition.setWeight(Float.valueOf(10));
        thenPosition.setMinPosition(Integer.valueOf(7));
        thenPosition.setMaxPosition(Integer.valueOf(config.getLineWidth().getValue()));
        thenPosition.setFallbackPosition(CaseThenFallbackPositionOption.THEN_INDENTED);
        caseType.setThenPosition(thenPosition);
        caseType.setElsePosition(CaseElsePositionOption.ELSE_UNDER_THEN);
        caseType.setEndPosition(CaseEndPositionOption.END_UNDER_CASE);
        config.setCaseOperand(caseType);

        /*
         * case when condition then ...
         */
        caseType = factory.createCaseType();
        integerValue = factory.createIntegerValueOption();
        integerValue.setValue(60);
        integerValue.setWeight(Float.valueOf(5));
        caseType.setMaxSingleLineClause(integerValue);
        whenPosition = factory.createCaseWhenPositionType();
        whenPosition.setValue(CaseWhenPositionOption.WHEN_AFTER_CASE);
        whenPosition.setWeight(Float.valueOf(10));
        caseType.setWhenPosition(whenPosition);
        thenPosition = factory.createCaseThenPositionType();
        thenPosition.setValue(CaseThenPositionOption.THEN_AFTER_WHEN_ALIGNED);
        thenPosition.setWeight(Float.valueOf(10));
        caseType.setThenPosition(thenPosition);
        caseType.setElsePosition(CaseElsePositionOption.ELSE_UNDER_THEN);
        caseType.setEndPosition(CaseEndPositionOption.END_UNDER_CASE);
        config.setCaseWhen(caseType);
        thenPosition.setMinPosition(Integer.valueOf(7));
        thenPosition.setMaxPosition(Integer.valueOf(config.getLineWidth().getValue()));
        thenPosition.setFallbackPosition(CaseThenFallbackPositionOption.THEN_INDENTED);

        /*
         * language plpgsql
         */
        PlpgsqlType languagePlpgsql = factory.createPlpgsqlType();
        config.setLanguagePlpgsql(languagePlpgsql);
        /*
         * plpgsql declare section
         */
        PlpgsqlDeclareSectionType plpgsqlDeclareSectionType = factory.createPlpgsqlDeclareSectionType();
        languagePlpgsql.setDeclareSection(plpgsqlDeclareSectionType);
        PlpgsqlDeclareDataTypePosition plpgsqlDeclareDataTypePosition = factory.createPlpgsqlDeclareDataTypePosition();
        plpgsqlDeclareSectionType.setDataTypePosition(plpgsqlDeclareDataTypePosition);
        plpgsqlDeclareDataTypePosition.setAlignment(RelativePositionTypeEnum.VERTICALLY_ALIGNED);
        plpgsqlDeclareDataTypePosition.setMinPosition(Integer.valueOf(30));
        plpgsqlDeclareDataTypePosition.setMaxPosition(Integer.valueOf(60));
        plpgsqlDeclareDataTypePosition.setConstantPosition(PlpgsqlDeclareConstantPositionType.ALIGNED_BEFORE_DATA_TYPE);

        /*
         * plpgsql code section
         */
        PlpgsqlCodeSectionType plpgsqlCodeSection = factory.createPlpgsqlCodeSectionType();
        languagePlpgsql.setCodeSection(plpgsqlCodeSection);
        PlpgsqlIfStatementType ifStatement = factory.createPlpgsqlIfStatementType();
        plpgsqlCodeSection.setIfStatement(ifStatement);
        ifStatement.setThen(PlpgsqlConditionEndPositionType.SINGLE_LINE_AFTER_MULTI_LINE_UNDER);
        ifStatement.setConditionIndent(CommaSeparatedListIndentOption.INDENTED);
        PlpgsqlForStatementType forStatement = factory.createPlpgsqlForStatementType();
        plpgsqlCodeSection.setForStatement(forStatement);
        forStatement.setLoop(PlpgsqlConditionEndPositionType.SINGLE_LINE_AFTER_MULTI_LINE_UNDER);

        return config;
    }

    /**
     * Figure out the base directory of this project. Source and target files will be relative to this directory
     *
     * @return Path The project directory
     */
    private static Path getProjectDirectory() {
        Path projectDirectory = Paths.get(System.getProperty("user.dir"));
        Path childDir = Paths.get(System.getProperty("user.dir"), "pgcode_formatter");
        /*
         * May be in the parent directory
         */
        if (Files.isDirectory(childDir)) {
            projectDirectory = childDir;
        }
        return projectDirectory.toAbsolutePath();
    }

}
