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
import org.junit.jupiter.api.Test;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.Configuration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.structure.WithQuery;

/**
 * Tests a WithQuery that is supposed to be part of a WITH statement
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestWithQuery {
    @Test
    public void simplestSingleLine() throws IOException {
        String src = "a as (select 'x')";
        String output = null;
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            WithQuery q = new WithQuery(postgresInputReader.getFirstResult());
            RenderResult renderResult = q.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
            Assertions.assertEquals("a as (select 'x')", output);
        }
    }

    @Test
    public void smallColumnListSingleLine() throws IOException {
        String src = "a (col_1) as (select 'x')";
        String output = null;
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            WithQuery q = new WithQuery(postgresInputReader.getFirstResult());
            Assertions.assertEquals(src, q.getText(), "getText()");
            RenderResult renderResult = q.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
            Assertions.assertEquals("a (col_1) as (select 'x')", output);
        }
    }

    @Test
    public void longColumnList() throws IOException {
        String src = "a_somewhat_long_name (some_long_column_name, another_long_column_name, and_another_name_also_long"
                + ", \"and a long double quoted column name\""
                + ", \"and let's do another long double quoted column name\") as materialized (select 'x')";
        String output = null;
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            WithQuery q = new WithQuery(postgresInputReader.getFirstResult());
            Assertions.assertEquals(src, q.getText(), "getText()");
            RenderResult renderResult = q.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
//@formatter:off
            Assertions.assertEquals(
                    "a_somewhat_long_name ( some_long_column_name, another_long_column_name\n" + 
                    "                     , and_another_name_also_long\n" + 
                    "                     , \"and a long double quoted column name\"\n" + 
                    "                     , \"and let's do another long double quoted column name\" ) as materialized (select 'x')",
                    output);
//@formatter:on
        }
    }

    @Test
    public void longNamelongColumnList() throws IOException {
        String src = "\"this is a with query with an absurdly long double quoted name to enforce a line break\""
                + " (some_long_column_name, another_long_column_name, and_another_name_also_long"
                + ", \"and a long double quoted column name\""
                + ", \"and let's do another long double quoted column name\") as not materialized"
                + " (select 'a somewhat longer text literal')";
        String output = null;
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            WithQuery q = new WithQuery(postgresInputReader.getFirstResult());
            Assertions.assertEquals(src, q.getText(), "getText()");
            RenderResult renderResult = q.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
//@formatter:off
            Assertions.assertEquals(
                    "\"this is a with query with an absurdly long double quoted name to enforce a line break\"\n" + 
                    "    ( some_long_column_name, another_long_column_name\n" + 
                    "    , and_another_name_also_long\n" + 
                    "    , \"and a long double quoted column name\"\n" + 
                    "    , \"and let's do another long double quoted column name\" ) as not materialized (select 'a somewhat longer text literal')", output);
//@formatter:on
        }
    }

}
