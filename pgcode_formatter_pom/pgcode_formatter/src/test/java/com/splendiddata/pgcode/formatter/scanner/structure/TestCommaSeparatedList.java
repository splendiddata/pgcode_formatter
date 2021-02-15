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

package com.splendiddata.pgcode.formatter.scanner.structure;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.BeforeOrAfterType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CommaSeparatedListGroupingType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CommaSeparatedListIndentOption;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CommaSeparatedListIndentType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.Configuration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.IntegerValueOption;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.ObjectFactory;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.Util;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Some JUnit tests for a CommaSeparatedList
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestCommaSeparatedList {
    private static CommaSeparatedList testList;
    private static final ObjectFactory settingsFactory = new ObjectFactory();
    private static FormatConfiguration config;

    @BeforeAll
    public static final void beforeAll() throws IOException {
        String src = "'element 1', 'element 2', 'element 3', 'element 4', 'element 5', 'element 6', 'element 7', 'element 8', 'element 9', 'element 10'";
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(src))) {
            testList = CommaSeparatedList.withArbitraryEnd(reader.getFirstResult(),
                    node -> PostgresInputReader.interpretStatementBody(node), node -> false);
        }
        config = new FormatConfiguration((Configuration) null);
    }

    @Test
    public void someWeights() {
        FormatContext formatContext = new FormatContext(config, null);
        CommaSeparatedListGroupingType csListConfig = settingsFactory.createCommaSeparatedListGroupingType();
        csListConfig.setCommaBeforeOrAfter(BeforeOrAfterType.BEFORE);
        CommaSeparatedListIndentType indentType = settingsFactory.createCommaSeparatedListIndentType();
        indentType.setValue(CommaSeparatedListIndentOption.UNDER_FIRST_ARGUMENT);
        indentType.setWeight(Float.valueOf(5));
        csListConfig.setIndent(indentType);
        IntegerValueOption maxArgumentsPerGroup = settingsFactory.createIntegerValueOption();
        maxArgumentsPerGroup.setValue(4);
        maxArgumentsPerGroup.setWeight(Float.valueOf(5));
        csListConfig.setMaxArgumentsPerGroup(maxArgumentsPerGroup);
        IntegerValueOption maxGroupLength = settingsFactory.createIntegerValueOption();
        maxGroupLength.setValue(30);
        maxGroupLength.setWeight(Float.valueOf(5));
        csListConfig.setMaxLengthOfGroup(maxGroupLength);
        IntegerValueOption maxSingleLineLength = settingsFactory.createIntegerValueOption();
        maxSingleLineLength.setValue(150);
        maxSingleLineLength.setWeight(Float.valueOf(10));
        csListConfig.setMaxSingleLineLength(maxSingleLineLength);
        csListConfig.setMultilineClosingParenOnNewLine(Boolean.FALSE);
        csListConfig.setMultilineOpeningParenBeforeArgument(Boolean.TRUE);
        formatContext.setCommaSeparatedListGrouping(csListConfig);

        Assertions.assertEquals(
                "'element 1', 'element 2', 'element 3', 'element 4', 'element 5', 'element 6', 'element 7', 'element 8', 'element 9', 'element 10'",
                testList.beautify(formatContext,null, config).beautify(), Util.xmlBeanToString(csListConfig));

        maxSingleLineLength.setValue(100);
        Assertions.assertEquals(
                "'element 1', 'element 2'\n, 'element 3', 'element 4'\n, 'element 5', 'element 6'\n, 'element 7', 'element 8'\n, 'element 9', 'element 10'",
                testList.beautify(formatContext, null, config).beautify(), Util.xmlBeanToString(csListConfig));

        maxSingleLineLength.setWeight(Float.valueOf(5));
        Assertions.assertEquals(
                "'element 1', 'element 2'\n, 'element 3', 'element 4'\n, 'element 5', 'element 6'\n, 'element 7', 'element 8'\n, 'element 9', 'element 10'",
                testList.beautify(formatContext, null, config).beautify(), Util.xmlBeanToString(csListConfig));

        maxArgumentsPerGroup.setWeight(Float.valueOf(10));
        Assertions.assertEquals(
                "'element 1', 'element 2', 'element 3', 'element 4'\n, 'element 5', 'element 6', 'element 7', 'element 8'\n, 'element 9', 'element 10'",
                testList.beautify(formatContext, null, config).beautify(), Util.xmlBeanToString(csListConfig));

        maxArgumentsPerGroup.setWeight(Float.valueOf(5));
        maxGroupLength.setValue(40);
        Assertions.assertEquals(
                "'element 1', 'element 2', 'element 3'\n, 'element 4', 'element 5', 'element 6'\n, 'element 7', 'element 8', 'element 9'\n, 'element 10'",
                testList.beautify(formatContext, null, config).beautify(), Util.xmlBeanToString(csListConfig));

        maxGroupLength.setValue(70);
        Assertions.assertEquals(
                "'element 1', 'element 2', 'element 3', 'element 4'\n, 'element 5', 'element 6', 'element 7', 'element 8'\n, 'element 9', 'element 10'",
                testList.beautify(formatContext, null, config).beautify(), Util.xmlBeanToString(csListConfig));

        indentType.setWeight(Float.valueOf(6));
        Assertions.assertEquals(
                "'element 1'\n, 'element 2'\n, 'element 3'\n, 'element 4'\n, 'element 5'\n, 'element 6'\n, 'element 7'\n, 'element 8'\n, 'element 9'\n, 'element 10'",
                testList.beautify(formatContext, null, config).beautify(), Util.xmlBeanToString(csListConfig));

        maxArgumentsPerGroup.setWeight(Float.valueOf(6));
        Assertions.assertEquals(
                "'element 1', 'element 2', 'element 3', 'element 4'\n, 'element 5', 'element 6', 'element 7', 'element 8'\n, 'element 9', 'element 10'",
                testList.beautify(formatContext, null, config).beautify(), Util.xmlBeanToString(csListConfig));

        maxGroupLength.setWeight(Float.valueOf(7));
        Assertions.assertEquals(
                "'element 1', 'element 2', 'element 3', 'element 4', 'element 5'\n, 'element 6', 'element 7', 'element 8', 'element 9', 'element 10'",
                testList.beautify(formatContext, null, config).beautify(), Util.xmlBeanToString(csListConfig));

        maxGroupLength.setWeight(Float.valueOf(6));
        maxGroupLength.setValue(40);
        Assertions.assertEquals(
                "'element 1', 'element 2', 'element 3'\n, 'element 4', 'element 5', 'element 6'\n, 'element 7', 'element 8', 'element 9'\n, 'element 10'",
                testList.beautify(formatContext, null, config).beautify(), Util.xmlBeanToString(csListConfig));
    }

    @Test
    public void testSomeNesting() throws IOException {
        FormatContext formatContext = new FormatContext(config, null);

        String src = "((nv.oid = v.relnamespace) AND (v.relkind = 'v'::\"char\"))";
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(src))) {
            CommaSeparatedList csl = CommaSeparatedList.withArbitraryEnd(reader.getFirstResult(),
                    node -> PostgresInputReader.interpretStatementBody(node), node -> {
                        if (!node.is(ScanResultType.IDENTIFIER)) {
                            return false;
                        }
                        switch (node.toString().toLowerCase()) {
                        case "where":
                        case "group":
                        case "having":
                        case "window":
                        case "union":
                        case "intersect":
                        case "except":
                        case "order":
                        case "limit":
                        case "offset":
                        case "fetch":
                        case "for":
                        case "into":
                            return true;
                        default:
                            return false;
                        }
                    });
            Assertions.assertEquals(src, csl.toString(), "toString()");
            Assertions.assertEquals(src, csl.beautify(formatContext, null, config).beautify(),
                    "csl.beautify(formatContext, config).beautify()");
        }
    }

}
