/*
 * Copyright (c) Splendid Data Product Development B.V. 2020
 *
 * This program is free software: You may redistribute and/or modify under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at Client's option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, Client should obtain one via www.gnu.org/licenses/.
 */

package com.splendiddata.pgcode.formatter;

import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.BaseFormatOption;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.BooleanOption;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CaseThenPositionType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CaseType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CaseWhenPositionType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.ColumnConstraintRelativePositionType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CommaSeparatedListGroupingType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CommaSeparatedListIndentType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.Configuration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.FromItemGroupingType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.FromItemIndentType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.FunctionDefinitionArgumentGroupingType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.IndentType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.IntegerRangeOption;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.IntegerValueOption;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.LogicalOperatorsIndentType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.ObjectFactory;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.PlpgsqlCodeSectionType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.PlpgsqlDeclareDataTypePosition;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.PlpgsqlDeclareSectionType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.PlpgsqlForStatementType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.PlpgsqlIfStatementType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.PlpgsqlType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.QueryConfigType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.RelativePositionType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.TableDefinitionType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.TabsType;
import com.splendiddata.pgcode.formatter.internal.Dicts;

/**
 * Utility class for the code formatter.
 */
public class ConfigUtil {
    public static String lineBreak = "\n";

    public static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    /**
     * Checks whether the provided token is a postgres major keyword.
     * 
     * @param token
     *            The token to check
     * @return true if the provided token is a postgres major keyword.
     */
    public static boolean isMajorKeywords(String token) {
        return Dicts.pgMajorKeywords.contains(token.toUpperCase());
    }

    /**
     * Checks whether the provided token is not a postgres function.
     * 
     * @param token
     *            The token to check
     * @return true if the provided token is not a postgres function.
     */
    public static boolean isKeywordNotFunctionCall(String token) {
        return Dicts.pgKeywordNotFunctionCall.contains(token.toUpperCase());
    }

    /**
     * deep copy {@link Configuration }
     * 
     * @param original
     *            the Configuration to copy. May be null.
     * @return Configuration the deep copied original or null if the original is null
     * 
     */
    public static Configuration copy(Configuration original) {
        if (original == null) {
            return null;
        }
        Configuration copy = OBJECT_FACTORY.createConfiguration();
        copy.setCaseOperand(copy(original.getCaseOperand()));
        copy.setCaseWhen(copy(original.getCaseWhen()));
        copy.setCommaSeparatedListGrouping(copy(original.getCommaSeparatedListGrouping()));
        copy.setEmptyLine(original.getEmptyLine());
        copy.setFromItemGrouping(copy(original.getFromItemGrouping()));
        copy.setFunctionCallArgumentGrouping(copy(original.getFunctionCallArgumentGrouping()));
        copy.setLogicalOperatorsIndent(copy(original.getLogicalOperatorsIndent()));
        copy.setFunctionDefinitionArgumentGrouping(copy(original.getFunctionDefinitionArgumentGrouping()));
        copy.setIndent(copy(original.getIndent()));
        copy.setLanguagePlpgsql(copy(original.getLanguagePlpgsql()));
        copy.setLetterCaseFunctions(original.getLetterCaseFunctions());
        copy.setLetterCaseKeywords(original.getLetterCaseKeywords());
        copy.setLineWidth(copy(original.getLineWidth()));
        copy.setQueryConfig(copy(original.getQueryConfig()));
        copy.setTableDefinition(copy(original.getTableDefinition()));
        copy.setTabs(copy(original.getTabs()));
        copy.setTargetListGrouping(copy(original.getTargetListGrouping()));
        return copy;
    }

    /**
     * deep copy {@link IntegerValueOption }
     * 
     * @param original
     *            the IntegerValueOption to copy. May be null.
     * @return IntegerValueOption the deep copied original or null if the original is null
     * 
     */
    public static IntegerValueOption copy(IntegerValueOption original) {
        if (original == null) {
            return null;
        }
        IntegerValueOption copy = OBJECT_FACTORY.createIntegerValueOption();
        copy.setValue(original.getValue());
        copy.setWeight(original.getWeight());
        return copy;
    }

    /**
     * deep copy {@link TabsType }
     * 
     * @param original
     *            the TabsType to copy. May be null.
     * @return TabsType the deep copied original or null if the original is null
     * 
     */
    public static TabsType copy(TabsType original) {
        if (original == null) {
            return null;
        }
        TabsType copy = OBJECT_FACTORY.createTabsType();
        copy.setTabsOrSpaces(original.getTabsOrSpaces());
        copy.setTabWidth(original.getTabWidth());
        return copy;
    }

    /**
     * deep copy {@link IndentType }
     * 
     * @param original
     *            the IndentType to copy. May be null.
     * @return IndentType the deep copied original or null if the original is null
     * 
     */
    public static IndentType copy(IndentType original) {
        if (original == null) {
            return null;
        }
        IndentType copy = OBJECT_FACTORY.createIndentType();
        copy.setIndentWidth(original.getIndentWidth());
        copy.setTabsOrSpaces(original.getTabsOrSpaces());
        return copy;
    }

    /**
     * deep copy {@link QueryConfigType }
     * 
     * @param original
     *            the QueryConfigType to copy. May be null.
     * @return QueryConfigType the deep copied original or null if the original is null
     * 
     */
    public static QueryConfigType copy(QueryConfigType original) {
        if (original == null) {
            return null;
        }
        QueryConfigType copy = OBJECT_FACTORY.createQueryConfigType();
        copy.setMaxSingleLineQuery(copy(original.getMaxSingleLineQuery()));
        copy.setIndent(original.isIndent());
        copy.setMajorKeywordsOnSeparateLine(original.isMajorKeywordsOnSeparateLine());
        return copy;
    }

    /**
     * deep copy {@link CommaSeparatedListGroupingType }
     * 
     * @param original
     *            the CommaSeparatedListGroupingType to copy. May be null.
     * @return CommaSeparatedListGroupingType the deep copied original or null if the original is null
     * 
     */
    public static CommaSeparatedListGroupingType copy(CommaSeparatedListGroupingType original) {
        if (original == null) {
            return null;
        }
        CommaSeparatedListGroupingType copy = OBJECT_FACTORY.createCommaSeparatedListGroupingType();
        copy.setCommaBeforeOrAfter(original.getCommaBeforeOrAfter());
        copy.setIndent(copy(original.getIndent()));
        copy.setMaxArgumentsPerGroup(copy(original.getMaxArgumentsPerGroup()));
        copy.setMaxLengthOfGroup(copy(original.getMaxLengthOfGroup()));
        copy.setMaxSingleLineLength(copy(original.getMaxSingleLineLength()));
        copy.setMultilineClosingParenOnNewLine(original.isMultilineClosingParenOnNewLine());
        copy.setMultilineOpeningParenBeforeArgument(original.isMultilineOpeningParenBeforeArgument());
        return copy;
    }

    /**
     * deep copy {@link LogicalOperatorsIndentType }
     *
     * @param original
     *            the LogicalOperatorsIndentType to copy. May be null.
     * @return LogicalOperatorsIndentType the deep copied original or null if the original is null
     *
     */
    public static LogicalOperatorsIndentType copy(LogicalOperatorsIndentType original) {
        if (original == null) {
            return null;
        }
        LogicalOperatorsIndentType copy = OBJECT_FACTORY.createLogicalOperatorsIndentType();
        copy.setIndent(original.getIndent());
        return copy;
    }

    /**
     * deep copy {@link FunctionDefinitionArgumentGroupingType }
     * 
     * @param original
     *            the FunctionDefinitionArgumentGroupingType to copy. May be null.
     * @return FunctionDefinitionArgumentGroupingType the deep copied original or null if the original is null
     * 
     */
    public static FunctionDefinitionArgumentGroupingType copy(FunctionDefinitionArgumentGroupingType original) {
        if (original == null) {
            return null;
        }
        FunctionDefinitionArgumentGroupingType copy = OBJECT_FACTORY.createFunctionDefinitionArgumentGroupingType();
        copy.setArgumentGrouping(copy(original.getArgumentGrouping()));
        copy.setArgumentName(copy(original.getArgumentName()));
        copy.setDataType(copy(original.getDataType()));
        copy.setDefaultIndicator(original.getDefaultIndicator());
        copy.setDefaultValue(copy(original.getDefaultValue()));
        return copy;
    }

    /**
     * deep copy {@link TableDefinitionType }
     * 
     * @param original
     *            the TableDefinitionType to copy. May be null.
     * @return TableDefinitionType the deep copied original or null if the original is null
     * 
     */
    public static TableDefinitionType copy(TableDefinitionType original) {
        if (original == null) {
            return null;
        }
        TableDefinitionType copy = OBJECT_FACTORY.createTableDefinitionType();
        copy.setArgumentGrouping(copy(original.getArgumentGrouping()));
        copy.setColumnContraint(copy(original.getColumnContraint()));
        copy.setDataType(copy(original.getDataType()));
        return copy;
    }

    /**
     * deep copy {@link FromItemGroupingType }
     * 
     * @param original
     *            the FromItemGroupingType to copy. May be null.
     * @return FromItemGroupingType the deep copied original or null if the original is null
     * 
     */
    public static FromItemGroupingType copy(FromItemGroupingType original) {
        if (original == null) {
            return null;
        }
        FromItemGroupingType copy = OBJECT_FACTORY.createFromItemGroupingType();
        copy.setAliasAlignment(copy(original.getAliasAlignment()));
        copy.setComma(original.getComma());
        copy.setMaxSingleLineLength(copy(original.getMaxSingleLineLength()));
        copy.setMultilineClosingParenOnNewLine(original.isMultilineClosingParenOnNewLine());
        copy.setMultilineOpeningParenBeforeArgument(original.isMultilineOpeningParenBeforeArgument());
        return copy;
    }

    /**
     * deep copy {@link CaseType }
     * 
     * @param original
     *            the CaseType to copy. May be null.
     * @return CaseType the deep copied original or null if the original is null
     * 
     */
    public static CaseType copy(CaseType original) {
        if (original == null) {
            return null;
        }
        CaseType copy = OBJECT_FACTORY.createCaseType();
        copy.setElsePosition(original.getElsePosition());
        copy.setEndPosition(original.getEndPosition());
        copy.setMaxSingleLineClause(copy(original.getMaxSingleLineClause()));
        copy.setThenPosition(copy(original.getThenPosition()));
        copy.setWhenPosition(copy(original.getWhenPosition()));
        return copy;
    }

    /**
     * deep copy {@link PlpgsqlType }
     * 
     * @param original
     *            the PlpgsqlType to copy. May be null.
     * @return PlpgsqlType the deep copied original or null if the original is null
     * 
     */
    public static PlpgsqlType copy(PlpgsqlType original) {
        if (original == null) {
            return null;
        }
        PlpgsqlType copy = OBJECT_FACTORY.createPlpgsqlType();
        copy.setCodeSection(copy(original.getCodeSection()));
        copy.setDeclareSection(copy(original.getDeclareSection()));
        return copy;
    }

    /**
     * deep copy {@link BaseFormatOption }
     * 
     * @param original
     *            the BaseFormatOption to copy. May be null.
     * @return BaseFormatOption the deep copied original or null if the original is null
     * 
     */
    public static BaseFormatOption copy(BaseFormatOption original) {
        if (original == null) {
            return null;
        }
        BaseFormatOption copy = OBJECT_FACTORY.createBaseFormatOption();
        copy.setWeight(original.getWeight());
        return copy;
    }

    /**
     * deep copy {@link BooleanOption }
     * 
     * @param original
     *            the BooleanOption to copy. May be null.
     * @return BooleanOption the deep copied original or null if the original is null
     * 
     */
    public static BooleanOption copy(BooleanOption original) {
        if (original == null) {
            return null;
        }
        BooleanOption copy = OBJECT_FACTORY.createBooleanOption();
        copy.setWeight(original.getWeight());
        copy.setValue(original.isValue());
        return copy;
    }

    /**
     * deep copy {@link IntegerRangeOption }
     * 
     * @param original
     *            the IntegerRangeOption to copy. May be null.
     * @return IntegerRangeOption the deep copied original or null if the original is null
     * 
     */
    public static IntegerRangeOption copy(IntegerRangeOption original) {
        if (original == null) {
            return null;
        }
        IntegerRangeOption copy = OBJECT_FACTORY.createIntegerRangeOption();
        copy.setRangeFrom(original.getRangeFrom());
        copy.setRangeUntil(original.getRangeUntil());
        copy.setWeight(original.getWeight());
        return copy;
    }

    /**
     * deep copy {@link CommaSeparatedListIndentType }
     * 
     * @param original
     *            the CommaSeparatedListIndentType to copy. May be null.
     * @return CommaSeparatedListIndentType the deep copied original or null if the original is null
     * 
     */
    public static CommaSeparatedListIndentType copy(CommaSeparatedListIndentType original) {
        if (original == null) {
            return null;
        }
        CommaSeparatedListIndentType copy = OBJECT_FACTORY.createCommaSeparatedListIndentType();
        copy.setValue(original.getValue());
        copy.setWeight(original.getWeight());
        return copy;
    }

    /**
     * deep copy {@link RelativePositionType }
     * 
     * @param original
     *            the RelativePositionType to copy. May be null.
     * @return RelativePositionType the deep copied original or null if the original is null
     * 
     */
    public static RelativePositionType copy(RelativePositionType original) {
        if (original == null) {
            return null;
        }
        RelativePositionType copy = OBJECT_FACTORY.createRelativePositionType();
        copy.setAlignment(original.getAlignment());
        copy.setMaxPosition(original.getMaxPosition());
        copy.setMinPosition(original.getMinPosition());
        return copy;
    }

    /**
     * deep copy {@link ColumnConstraintRelativePositionType }
     * 
     * @param original
     *            the ColumnConstraintRelativePositionType to copy. May be null.
     * @return ColumnConstraintRelativePositionType the deep copied original or null if the original is null
     * 
     */
    public static ColumnConstraintRelativePositionType copy(ColumnConstraintRelativePositionType original) {
        if (original == null) {
            return null;
        }
        ColumnConstraintRelativePositionType copy = OBJECT_FACTORY.createColumnConstraintRelativePositionType();
        copy.setAlignment(original.getAlignment());
        copy.setMaxPosition(original.getMaxPosition());
        copy.setMinPosition(original.getMinPosition());
        return copy;
    }

    /**
     * deep copy {@link FromItemIndentType }
     * 
     * @param original
     *            the FromItemIndentType to copy. May be null.
     * @return FromItemIndentType the deep copied original or null if the original is null
     * 
     */
    public static FromItemIndentType copy(FromItemIndentType original) {
        if (original == null) {
            return null;
        }
        FromItemIndentType copy = OBJECT_FACTORY.createFromItemIndentType();
        copy.setValue(original.getValue());
        copy.setWeight(original.getWeight());
        return copy;
    }

    /**
     * deep copy {@link CaseWhenPositionType }
     * 
     * @param original
     *            the CaseWhenPositionType to copy. May be null.
     * @return CaseWhenPositionType the deep copied original or null if the original is null
     * 
     */
    public static CaseWhenPositionType copy(CaseWhenPositionType original) {
        if (original == null) {
            return null;
        }
        CaseWhenPositionType copy = OBJECT_FACTORY.createCaseWhenPositionType();
        copy.setValue(original.getValue());
        copy.setWeight(original.getWeight());
        return copy;
    }

    /**
     * deep copy {@link CaseThenPositionType }
     * 
     * @param original
     *            the CaseThenPositionType to copy. May be null.
     * @return CaseThenPositionType the deep copied original or null if the original is null
     * 
     */
    public static CaseThenPositionType copy(CaseThenPositionType original) {
        if (original == null) {
            return null;
        }
        CaseThenPositionType copy = OBJECT_FACTORY.createCaseThenPositionType();
        copy.setValue(original.getValue());
        copy.setWeight(original.getWeight());
        return copy;
    }

    /**
     * deep copy {@link PlpgsqlDeclareDataTypePosition }
     * 
     * @param original
     *            the PlpgsqlDeclareDataTypePosition to copy. May be null.
     * @return PlpgsqlDeclareDataTypePosition the deep copied original or null if the original is null
     * 
     */
    public static PlpgsqlDeclareDataTypePosition copy(PlpgsqlDeclareDataTypePosition original) {
        if (original == null) {
            return null;
        }
        PlpgsqlDeclareDataTypePosition copy = OBJECT_FACTORY.createPlpgsqlDeclareDataTypePosition();
        copy.setAlignment(original.getAlignment());
        copy.setConstantPosition(original.getConstantPosition());
        copy.setMaxPosition(original.getMaxPosition());
        copy.setMinPosition(original.getMinPosition());
        return copy;
    }

    /**
     * deep copy {@link PlpgsqlDeclareSectionType }
     * 
     * @param original
     *            the PlpgsqlDeclareSectionType to copy. May be null.
     * @return PlpgsqlDeclareSectionType the deep copied original or null if the original is null
     * 
     */
    public static PlpgsqlDeclareSectionType copy(PlpgsqlDeclareSectionType original) {
        if (original == null) {
            return null;
        }
        PlpgsqlDeclareSectionType copy = OBJECT_FACTORY.createPlpgsqlDeclareSectionType();
        copy.setDataTypePosition(copy(original.getDataTypePosition()));
        return copy;
    }

    /**
     * deep copy {@link PlpgsqlIfStatementType }
     * 
     * @param original
     *            the PlpgsqlIfStatementType to copy. May be null.
     * @return PlpgsqlIfStatementType the deep copied original or null if the original is null
     * 
     */
    public static PlpgsqlIfStatementType copy(PlpgsqlIfStatementType original) {
        if (original == null) {
            return null;
        }
        PlpgsqlIfStatementType copy = OBJECT_FACTORY.createPlpgsqlIfStatementType();
        copy.setConditionIndent(original.getConditionIndent());
        copy.setThen(original.getThen());
        return copy;
    }

    /**
     * deep copy {@link PlpgsqlForStatementType }
     * 
     * @param original
     *            the PlpgsqlForStatementType to copy. May be null.
     * @return PlpgsqlForStatementType the deep copied original or null if the original is null
     * 
     */
    public static PlpgsqlForStatementType copy(PlpgsqlForStatementType original) {
        if (original == null) {
            return null;
        }
        PlpgsqlForStatementType copy = OBJECT_FACTORY.createPlpgsqlForStatementType();
        copy.setLoop(original.getLoop());
        return copy;
    }

    /**
     * deep copy {@link PlpgsqlCodeSectionType }
     * 
     * @param original
     *            the PlpgsqlCodeSectionType to copy. May be null.
     * @return PlpgsqlCodeSectionType the deep copied original or null if the original is null
     * 
     */
    public static PlpgsqlCodeSectionType copy(PlpgsqlCodeSectionType original) {
        if (original == null) {
            return null;
        }
        PlpgsqlCodeSectionType copy = OBJECT_FACTORY.createPlpgsqlCodeSectionType();
        copy.setForStatement(copy(original.getForStatement()));
        copy.setIfStatement(copy(original.getIfStatement()));
        return copy;
    }
}
