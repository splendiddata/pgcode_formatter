/*
 * Copyright (c) Splendid Data Product Development B.V. 2020
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

package com.splendiddata.pgcode.formatter.scanner.structure;

import java.io.IOException;
import java.io.StringReader;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.splendiddata.pgcode.formatter.CodeFormatter;
import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.Configuration;

/**
 * JUnit tests for line comments.
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestCommentLine {

    @Test
    public void oneLineComment() throws IOException {
        String input = "-- this a line comment";
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        String output = CodeFormatter.toStringResults(new StringReader(input), config).collect(Collectors.joining());
        Assertions.assertEquals("-- this a line comment\n", output.toString(), "input: " + input);
    }

    @Test
    public void multipeLineComments() throws IOException {
        String input = "-- 1. this is a line comment\n" + "-- 2. this is a line comment";
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        String output = CodeFormatter.toStringResults(new StringReader(input), config).collect(Collectors.joining());
        Assertions.assertEquals("-- 1. this is a line comment\n" + "-- 2. this is a line comment\n", output.toString(),
                "input: " + input);
    }

    @Test
    public void lineCommentsEnd() throws IOException {
        String input =
        // @formatter:off
                "select" +
                " a -- 1. this is a line comment\n" +
                " , b -- 2. this is a line comment\n" +
                "from some_table where a > b order by 1, 2;";
        // @formatter:on
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        String output = CodeFormatter.toStringResults(new StringReader(input), config).collect(Collectors.joining());
        Assertions.assertEquals(
        // @formatter:off
                "select a -- 1. this is a line comment\n" +
                "     , b -- 2. this is a line comment\n" +
                "from some_table\n" +
                "where a > b\n" +
                "order by 1, 2;\n",
        // @formatter:off
                output.toString(),
                "input: " + input);
    }

    @Test
    public void lineCommentsStart() throws IOException {
        String input =
                // @formatter:off
                "select\n" +
                "-- 1. this is a line comment\n" +
                "a\n" +
                "-- 2. this is a line comment\n" +
                "b " +
                "from some_table where a > b order by 1, 2;";
        // @formatter:on
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        String output = CodeFormatter.toStringResults(new StringReader(input), config).collect(Collectors.joining());
        Assertions.assertEquals(
        // @formatter:off
                "select -- 1. this is a line comment\n"
                + "a -- 2. this is a line comment\n"
                + "b\n"
                + "from some_table\n"
                + "where a > b\n"
                + "order by 1, 2;\n",
                // @formatter:off
                output.toString(),
                "input: " + input);
    }

    @Test
    public void lineCommentsStartWithIndent1() throws IOException {
        String input =
                // @formatter:off
                "select\n" +
                "a,\n" +
                "-- 1. this is a line comment\n" +
                "b,\n" +
                "-- 2. this is a line comment\n" +
                "c " +
                "from some_table where a > b order by 1, 2;";
        // @formatter:on
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        String output = CodeFormatter.toStringResults(new StringReader(input), config).collect(Collectors.joining());
        Assertions.assertEquals(
        // @formatter:off
                "select a\n" +
                "     , -- 1. this is a line comment\n" +
                "       b\n" +
                "     , -- 2. this is a line comment\n" +
                "       c\n" +
                "from some_table\n" +
                "where a > b\n" +
                "order by 1, 2;\n",
                // @formatter:off
                output.toString(),
                "input: " + input);
    }

    @Test
    public void lineCommentsStartWithIndent2() throws IOException {
        String input =
                // @formatter:off
                "select\n" +
                "a\n" +
                "-- 1. this is a line comment\n" +
                ",b\n" +
                "-- 2. this is a line comment\n" +
                ",c " +
                "from some_table where a > b order by 1, 2;";
        // @formatter:on
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        String output = CodeFormatter.toStringResults(new StringReader(input), config).collect(Collectors.joining());
        Assertions.assertEquals(
        // @formatter:off
                "select a -- 1. this is a line comment\n"
                + "     , b -- 2. this is a line comment\n"
                + "     , c\n"
                + "from some_table\n"
                + "where a > b\n"
                + "order by 1, 2;\n",
                // @formatter:off
                output.toString(),
                "input: " + input);
    }

    @Test
    public void lineCommentsStartEnd() throws IOException {
        String input =
                // @formatter:off
                "select\n" +
                "a -- 1. this is a line comment\n" +
                "-- 1.1 this is a line comment\n" +
                ",b -- 2. this is a line comment\n" +
                "-- 2.2 this is a line comment\n" +
                ",c " +
                "from some_table where a > b order by 1, 2;";
        // @formatter:on
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        String output = CodeFormatter.toStringResults(new StringReader(input), config).collect(Collectors.joining());
        Assertions.assertEquals(
        // @formatter:off
                "select a -- 1. this is a line comment\n" +
                "         -- 1.1 this is a line comment\n" +
                "     , b -- 2. this is a line comment\n" +
                "         -- 2.2 this is a line comment\n" +
                "     , c\n" +
                "from some_table\n" +
                "where a > b\n" +
                "order by 1, 2;\n",
                // @formatter:off
                output.toString(),
                "input: " + input);
    }
}
