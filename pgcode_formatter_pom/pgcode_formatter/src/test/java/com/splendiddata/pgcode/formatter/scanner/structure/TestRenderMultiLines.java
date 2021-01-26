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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.Util;

/**
 * JUnit tests for {@link com.splendiddata.pgcode.formatter.internal.RenderMultiLines}
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestRenderMultiLines {
    private static FormatContext ctx;
    private static String indentation = "    ";

    private 
    @BeforeAll
    static void setUp() {
        // Just because it is needed when calling some methods
        ctx = new FormatContext(null, null);
    }

    /**
     * Construct a case clause that can be used for testing.
     * 
     * <pre>
     *  CASE
     *      WHEN my_col IS NULL
     *          THEN 'N'
     *      ELSE 'Y'
     *  END
     * </pre>
     *
     * @return
     */
    private RenderMultiLines constructCaseClause() {
        RenderMultiLines caseClause = new RenderMultiLines(null, ctx).setIndent(0);
        RenderMultiLines whenClause = new RenderMultiLines(null, ctx).setIndent(0);
        caseClause.putRenderResult(new RenderItem("CASE", RenderItemType.IDENTIFIER));
        caseClause.addLine(indentation);
        whenClause.putRenderResult(new RenderItem("WHEN", RenderItemType.IDENTIFIER));
        whenClause.addWhiteSpace();
        whenClause.putRenderResult(new RenderItem("my_col", RenderItemType.IDENTIFIER));
        whenClause.addWhiteSpace();
        whenClause.putRenderResult(new RenderItem("IS", RenderItemType.IDENTIFIER));
        whenClause.addWhiteSpace();
        whenClause.putRenderResult(new RenderItem("NULL", RenderItemType.IDENTIFIER));
        whenClause.addLine("        ");
        whenClause.putRenderResult(new RenderItem("THEN", RenderItemType.IDENTIFIER));
        whenClause.addWhiteSpace();
        whenClause.putRenderResult(new RenderItem("'N'", RenderItemType.IDENTIFIER));
        whenClause.addLine(indentation);
        whenClause.putRenderResult(new RenderItem("ELSE", RenderItemType.IDENTIFIER));
        whenClause.addWhiteSpace();
        whenClause.putRenderResult(new RenderItem("'Y'", RenderItemType.IDENTIFIER));
        caseClause.putRenderResult(whenClause);
        caseClause.addLine();
        caseClause.putRenderResult(new RenderItem("END", RenderItemType.IDENTIFIER));

        return caseClause;
    }

    @Test
    public void TestAddRenderResult() {
        RenderMultiLines resultTest1 = new RenderMultiLines(null, ctx);

        // Empty render result
        Assertions.assertEquals(0, resultTest1.getWidthFirstItem());

        resultTest1.addRenderResult(new RenderItem(Util.LF, RenderItemType.LINEFEED), ctx);
        Assertions.assertEquals(0, resultTest1.getRenderResults().size());

        resultTest1.addRenderResult(new RenderItem("SELECT", RenderItemType.IDENTIFIER), ctx);
        Assertions.assertEquals(1, resultTest1.getRenderResults().size());

        // Linefeed will not be added, use addLine() instead
        resultTest1.addRenderResult(new RenderItem(Util.LF, RenderItemType.LINEFEED), ctx);
        Assertions.assertEquals(2, resultTest1.getRenderResults().size());

        resultTest1.addLine();
        Assertions.assertEquals(3, resultTest1.getRenderResults().size());

    }

    @Test
    public void testAddLine() {
        RenderMultiLines result = new RenderMultiLines(null, ctx).setIndent(0);

        result.addLine();

        // A new line will not be added at the beginning of a render result
        Assertions.assertEquals(result.getRenderResults().size(), 0, "Render result should be empty");
        Assertions.assertEquals("", result.beautify());

        result.addRenderResult(new RenderItem("create", RenderItemType.IDENTIFIER), ctx);
        result.addLine();
        Assertions.assertEquals(3, result.getRenderResults().size());
        Assertions.assertEquals("create" + Util.LF, result.beautify());

        // A new line will not be added as a new line is already the last element in the render result
        result.addLine();
        Assertions.assertEquals(3, result.getRenderResults().size());
        Assertions.assertEquals("create" + Util.LF, result.beautify());
    }

    @Test
    public void testAddLineWithIndent() throws IOException {
        RenderMultiLines result = new RenderMultiLines(null, ctx).setIndent(0);

        result.addLine(" ");

        // Add a new line with indentation
        Assertions.assertEquals(2, result.getRenderResults().size());
        Assertions.assertEquals("\n ", result.beautify());

        // If a new line already exists, i.e. as last element,
        // then it will be replaced by a new line with indentation.
        result.addLine(indentation);
        Assertions.assertEquals("\n    ", result.beautify());

    }

    @Test
    public void testPutRenderResult() {
        RenderMultiLines result = new RenderMultiLines(null, ctx);

        // Just append the specified elements to the end
        // of the renderResults list of the RenderMultiLines result.
        result.putRenderResult(new RenderItem(Util.LF, RenderItemType.LINEFEED));
        result.putRenderResult(new RenderItem("create", RenderItemType.IDENTIFIER));
        Assertions.assertEquals(2, result.getRenderResults().size());
        Assertions.assertEquals(Util.LF + "create", result.beautify());
    }

    @Test
    public void testGetPosition() {
        RenderMultiLines result = new RenderMultiLines(null, ctx).setIndent(0);
        Assertions.assertEquals(0, result.getPosition());

        result.putRenderResult(new RenderItem(Util.LF, RenderItemType.LINEFEED));
        Assertions.assertEquals(0, result.getPosition());

        result.addLine(indentation); // add line and 4 spaces as indent of the next line
        // getPosition() takes account of the indentation
        Assertions.assertEquals(4, result.getPosition());

        result.putRenderResult(new RenderItem("create", RenderItemType.IDENTIFIER));
        result.addLine();
        result.putRenderResult(new RenderItem("table", RenderItemType.IDENTIFIER));
        Assertions.assertEquals(5, result.getPosition());

        result.addLine(indentation); // add line and 4 spaces as indent of the next line
        result.putRenderResult(new RenderItem("table_name", RenderItemType.IDENTIFIER));
        // getPosition() takes account of the indentation
        // size of "table_name" + 4 spaces --> 14
        Assertions.assertEquals(14, result.getPosition());

        result.addLine();
        Assertions.assertEquals(0, result.getPosition());
    }

    @Test
    public void testReplaceLast() {
        RenderMultiLines result = new RenderMultiLines(null, ctx).setIndent(0);
        result.putRenderResult(new RenderItem("create", RenderItemType.IDENTIFIER));
        result.addLine();
        Assertions.assertEquals("create" + Util.LF, result.beautify());

        result.replaceLast(new RenderItem(" ", RenderItemType.WHITESPACE));
        Assertions.assertEquals("create\n ", result.beautify());

        result.replaceLast(new RenderItem(Util.LF, RenderItemType.LINEFEED));
        Assertions.assertEquals("create\n\n", result.beautify());
    }

    @Test
    public void testAddWhiteSpaceIfNotExists() {
        RenderMultiLines result = new RenderMultiLines(null, ctx);
        result.addWhiteSpaceIfApplicable();
        Assertions.assertEquals(0, result.getRenderResults().size());
        Assertions.assertEquals("", result.beautify());

        result.putRenderResult(new RenderItem("create", RenderItemType.IDENTIFIER));
        result.addWhiteSpaceIfApplicable();
        Assertions.assertEquals("create ", result.beautify());

        // This white space will not be added
        result.addWhiteSpaceIfApplicable();
        Assertions.assertEquals("create ", result.beautify());
    }

    @Test
    public void testGetWidth() {

        RenderItem renderItem = new RenderItem("SELECT", RenderItemType.IDENTIFIER);
        Assertions.assertEquals(6, renderItem.getWidth());

        renderItem = new RenderItem(Util.LF, RenderItemType.LINEFEED);
        Assertions.assertEquals(0, renderItem.getWidth());

        RenderMultiLines resultTest1 = new RenderMultiLines(null, ctx);

        // Empty render result
        Assertions.assertEquals(0, resultTest1.getWidth());

        resultTest1.putRenderResult(new RenderItem("SELECT", RenderItemType.IDENTIFIER));
        Assertions.assertEquals(6, resultTest1.getWidth());

        resultTest1.addWhiteSpace();
        Assertions.assertEquals(7, resultTest1.getWidth());

        /**
         * <code>
         * SELECT CASE
         *     WHEN my_col IS NULL
         *         THEN 'N'
         *     ELSE 'Y'
         * END
         * </code>
         */
        resultTest1.putRenderResult(constructCaseClause());
        Assertions.assertEquals(23, resultTest1.getWidth());

        /**
         * <code>
         * SELECT DISTINCT ON (col) CASE
         *     WHEN my_col IS NULL
         *         THEN 'N'
         *     ELSE 'Y'
         * END
         * </code>
         */
        RenderMultiLines resultTest2 = new RenderMultiLines(null, ctx);
        resultTest2.putRenderResult(new RenderItem("SELECT", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(new RenderItem("DISTINCT", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(new RenderItem("ON", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(new RenderItem("(col)", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(constructCaseClause());
        Assertions.assertEquals(29, resultTest2.getWidth());
    }

    @Test
    public void testGetHeight() {
        RenderItem renderItem = new RenderItem("SELECT", RenderItemType.IDENTIFIER);
        Assertions.assertEquals(1, renderItem.getHeight());

        renderItem = new RenderItem(Util.LF, RenderItemType.LINEFEED);
        Assertions.assertEquals(1, renderItem.getHeight());

        RenderMultiLines resultTest1 = new RenderMultiLines(null, ctx);

        // Empty render result
        Assertions.assertEquals(0, resultTest1.getHeight());

        resultTest1.putRenderResult(new RenderItem("SELECT", RenderItemType.IDENTIFIER));
        Assertions.assertEquals(1, resultTest1.getHeight());
        resultTest1.addWhiteSpace();

        /**
         * <code>
         * SELECT CASE
         *     WHEN my_col IS NULL
         *         THEN 'N'
         *     ELSE 'Y'
         * END
         * </code>
         */
        resultTest1.putRenderResult(constructCaseClause());
        Assertions.assertEquals(5, resultTest1.getHeight());

        /**
         * <code>
         * SELECT DISTINCT ON (col) CASE
         *     WHEN my_col IS NULL
         *         THEN 'N'
         *     ELSE 'Y'
         * END
         * </code>
         */
        RenderMultiLines resultTest2 = new RenderMultiLines(null, ctx);
        resultTest2.putRenderResult(new RenderItem("SELECT", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(new RenderItem("DISTINCT", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(new RenderItem("ON", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(new RenderItem("(col)", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(constructCaseClause());
        Assertions.assertEquals(5, resultTest2.getHeight());

        RenderMultiLines resultTest3 = new RenderMultiLines(null, ctx);
        resultTest3.putRenderResult(new RenderItem("SELECT", RenderItemType.IDENTIFIER));
        resultTest3.addLine();
        Assertions.assertEquals(2, resultTest3.getHeight());

        RenderMultiLines resultTest4 = new RenderMultiLines(null, ctx);
        resultTest4.putRenderResult(new RenderItem(Util.LF, RenderItemType.LINEFEED));
        Assertions.assertEquals(2, resultTest4.getHeight());

    }

    @Test
    public void testGetWidthFirstLine() {
        RenderItem renderItem = new RenderItem("SELECT", RenderItemType.IDENTIFIER);
        Assertions.assertEquals(6, renderItem.getWidthFirstLine());

        renderItem = new RenderItem(Util.LF, RenderItemType.LINEFEED);
        Assertions.assertEquals(0, renderItem.getWidthFirstLine());

        RenderMultiLines resultTest1 = new RenderMultiLines(null, ctx);

        // Empty render result
        Assertions.assertEquals(0, resultTest1.getWidthFirstLine());

        resultTest1.putRenderResult(new RenderItem("SELECT", RenderItemType.IDENTIFIER));
        Assertions.assertEquals(6, resultTest1.getWidthFirstLine());
        resultTest1.addWhiteSpace();

        /**
         * <code>
         * SELECT CASE
         *     WHEN my_col IS NULL
         *         THEN 'N'
         *     ELSE 'Y'
         * END
         * </code>
         */
        resultTest1.putRenderResult(constructCaseClause());
        Assertions.assertEquals(11, resultTest1.getWidthFirstLine());

        /**
         * <code>
         * SELECT DISTINCT ON (col) CASE
         *     WHEN my_col IS NULL
         *         THEN 'N'
         *     ELSE 'Y'
         * END
         * </code>
         */
        RenderMultiLines resultTest2 = new RenderMultiLines(null, ctx);
        resultTest2.putRenderResult(new RenderItem("SELECT", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(new RenderItem("DISTINCT", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(new RenderItem("ON", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(new RenderItem("(col)", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(constructCaseClause());
        Assertions.assertEquals(29, resultTest2.getWidthFirstLine());

        RenderMultiLines resultTest3 = new RenderMultiLines(null, ctx);
        resultTest3.putRenderResult(new RenderItem("SELECT", RenderItemType.IDENTIFIER));
        resultTest3.addLine();
        Assertions.assertEquals(6, resultTest3.getWidthFirstLine());

        RenderMultiLines resultTest4 = new RenderMultiLines(null, ctx);
        resultTest4.putRenderResult(new RenderItem(Util.LF, RenderItemType.LINEFEED));
        Assertions.assertEquals(0, resultTest4.getWidthFirstLine());
    }

    @Test
    public void testGetWidthFirstItem() {
        RenderMultiLines resultTest1 = new RenderMultiLines(null, ctx);

        // Empty render result
        Assertions.assertEquals(0, resultTest1.getWidthFirstItem());

        resultTest1.putRenderResult(new RenderItem("SELECT", RenderItemType.IDENTIFIER));
        Assertions.assertEquals(6, resultTest1.getWidthFirstItem());
        resultTest1.addWhiteSpace();

        /**
         * <code>
         * SELECT CASE
         *     WHEN my_col IS NULL
         *         THEN 'N'
         *     ELSE 'Y'
         * END
         * </code>
         */
        resultTest1.putRenderResult(constructCaseClause());
        Assertions.assertEquals(6, resultTest1.getWidthFirstItem());

        /**
         * <code>
         * SELECT DISTINCT ON (col) CASE
         *     WHEN my_col IS NULL
         *         THEN 'N'
         *     ELSE 'Y'
         * END
         * </code>
         */
        RenderMultiLines resultTest2 = new RenderMultiLines(null, ctx);
        resultTest2.putRenderResult(new RenderItem("SELECT", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(new RenderItem("DISTINCT", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(new RenderItem("ON", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(new RenderItem("(col)", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(constructCaseClause());
        Assertions.assertEquals(6, resultTest2.getWidthFirstItem());

        RenderMultiLines resultTest3 = new RenderMultiLines(null, ctx);
        resultTest3.putRenderResult(new RenderItem("/* a block comment */", RenderItemType.COMMENT));
        resultTest3.addLine();
        Assertions.assertEquals(4, resultTest3.getWidthFirstItem());

        RenderMultiLines resultTest4 = new RenderMultiLines(null, ctx);
        resultTest4.putRenderResult(new RenderItem("-- a line comment", RenderItemType.COMMENT_LINE));
        resultTest4.addLine();
        Assertions.assertEquals(4, resultTest4.getWidthFirstItem());

        RenderMultiLines resultTest5 = new RenderMultiLines(null, ctx);
        resultTest5.putRenderResult(new RenderItem(Util.LF, RenderItemType.LINEFEED));
        Assertions.assertEquals(0, resultTest5.getWidthFirstItem());
    }

    @Test
    public void testIsLastNonWhiteSpaceEqualToLinefeed() {
        RenderMultiLines resultTest1 = new RenderMultiLines(null, ctx);

        // Empty render result
        Assertions.assertEquals(false, resultTest1.isLastNonWhiteSpaceEqualToLinefeed());

        resultTest1.putRenderResult(new RenderItem(Util.LF, RenderItemType.LINEFEED));
        Assertions.assertEquals(true, resultTest1.isLastNonWhiteSpaceEqualToLinefeed());

        resultTest1.putRenderResult(new RenderItem("SELECT", RenderItemType.IDENTIFIER));
        Assertions.assertEquals(false, resultTest1.isLastNonWhiteSpaceEqualToLinefeed());

        resultTest1.addLine();
        Assertions.assertEquals(true, resultTest1.isLastNonWhiteSpaceEqualToLinefeed());

        resultTest1.addWhiteSpace();
        Assertions.assertEquals(true, resultTest1.isLastNonWhiteSpaceEqualToLinefeed());

        RenderMultiLines resultTest2 = new RenderMultiLines(null, ctx);
        resultTest2.putRenderResult(new RenderItem("SELECT", RenderItemType.IDENTIFIER));
        RenderMultiLines temp = new RenderMultiLines(null, ctx);
        temp.putRenderResult(constructCaseClause());
        temp.addLine();
        temp.addWhiteSpace();
        resultTest2.putRenderResult(temp);
        resultTest2.addWhiteSpace();
        Assertions.assertEquals(true, resultTest2.isLastNonWhiteSpaceEqualToLinefeed());

        resultTest2.putRenderResult(new RenderItem("FROM", RenderItemType.IDENTIFIER));
        Assertions.assertEquals(false, resultTest2.isLastNonWhiteSpaceEqualToLinefeed());
    }


    @Test
    public void testIndentDefault() {
        RenderMultiLines resultTest1 = new RenderMultiLines(null, ctx);
        RenderItem renderItem = new RenderItem("SELECT", RenderItemType.IDENTIFIER);
        Assertions.assertEquals("SELECT", renderItem.beautify());

        // Latest linefeed will not be indented
        resultTest1.putRenderResult(new RenderItem("SELECT", RenderItemType.IDENTIFIER));
        resultTest1.addLine();
        Assertions.assertEquals("SELECT\n    ", resultTest1.beautify());

        resultTest1.putRenderResult(new RenderItem("*", RenderItemType.CHARACTER));
        Assertions.assertEquals("SELECT\n    *", resultTest1.beautify());

        /**
         * <code>
         * SELECT DISTINCT ON (col) CASE
         *     WHEN my_col IS NULL
         *         THEN 'N'
         *     ELSE 'Y'
         * END
         * </code>
         */
        RenderMultiLines resultTest2 = new RenderMultiLines(null, ctx);
        resultTest2.putRenderResult(new RenderItem("SELECT", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(new RenderItem("DISTINCT", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(new RenderItem("ON", RenderItemType.IDENTIFIER));
        resultTest2.addWhiteSpace();
        resultTest2.putRenderResult(new RenderItem("(col)", RenderItemType.IDENTIFIER));
        resultTest2.addLine();
        resultTest2.addRenderResult(constructCaseClause(), ctx);
        resultTest2.putRenderResult(new RenderItem(";", RenderItemType.SEMI_COLON));
        Assertions.assertEquals(
        // @formatter:off
                "SELECT DISTINCT ON (col)\n" +
                        "    CASE\n" +
                        "        WHEN my_col IS NULL\n" +
                        "            THEN 'N'\n" +
                        "        ELSE 'Y'\n" +
                        "    END;"
                , resultTest2.beautify());
        // @formatter:on

        RenderMultiLines resultTest3 = new RenderMultiLines(null, ctx);
        resultTest3.putRenderResult(new RenderItem("SELECT", RenderItemType.IDENTIFIER));
        resultTest3.addWhiteSpace();
        resultTest3.putRenderResult(new RenderItem("DISTINCT", RenderItemType.IDENTIFIER));
        resultTest3.addWhiteSpace();
        resultTest3.putRenderResult(new RenderItem("ON", RenderItemType.IDENTIFIER));
        resultTest3.addWhiteSpace();
        resultTest3.putRenderResult(new RenderItem("(col)", RenderItemType.IDENTIFIER));
        resultTest3.addLine();
        resultTest3.addRenderResult(constructCaseClause(), ctx);
        resultTest3.putRenderResult(new RenderItem(";", RenderItemType.SEMI_COLON));
        resultTest3.addLine();
        Assertions.assertEquals(
        // @formatter:off
                "SELECT DISTINCT ON (col)\n" +
                        "    CASE\n" +
                        "        WHEN my_col IS NULL\n" +
                        "            THEN 'N'\n" +
                        "        ELSE 'Y'\n" +
                        "    END;\n    "
                , resultTest3.beautify());
        // @formatter:on
    }
}
