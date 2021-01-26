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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.Configuration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.scanner.structure.CreateTableNode;

/**
 * JUnit tests for {@link CreateTableNode}
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestCreateTable {

    private static String[][] testCases() {
        return new String[][] { {
                // @formatter:off
                    // source
                    "CREATE table",
                    // expected
                    "CREATE table"
                    // @formatter:on
                }, {
                // @formatter:off
                    // source
                    "CREATE TABLE measurement\n" +
                    "        ( city_id         int not null" +
                    "        ,logdate             date not null\n" +
                    "        , peaktemp            int\n" +
                    "        , unitsales           int ) PARTITION BY RANGE (logdate);",
                    // expected
                    "CREATE TABLE measurement \n" +
                    "        ( city_id             int  not null\n" +
                    "        , logdate             date not null\n" +
                    "        , peaktemp            int\n" +
                    "        , unitsales           int )\n" +
                    "    PARTITION BY RANGE (logdate);"
                    // @formatter:on
                }, {
                // @formatter:off
                    // source
                    "CREATE TEMPORARY TABLE IF NOT EXIST measurement_y2008m02 PARTITION OF measurement FOR VALUES FROM ('2008-01-01') TO ('2008-02-01')",
                    // expected
                    "CREATE TEMPORARY TABLE IF NOT EXIST measurement_y2008m02\n" +
                    "    PARTITION OF measurement FOR VALUES FROM ('2008-01-01') TO ('2008-02-01')"
                    // @formatter:on
                } };
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testCreateTableStatement(String input, String expected) throws IOException {
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(input))) {
            CreateTableNode stmt = (CreateTableNode) PostgresInputReader
                    .interpretStatementStart(reader.getFirstResult());
            Assertions.assertEquals(input, stmt.toString(), "stmt.toString()");
            FormatConfiguration config = new FormatConfiguration((Configuration) null);
            String beautify = stmt.beautify(new FormatContext(config, null), null, config).beautify();
            Assertions.assertEquals(expected, beautify, "tried to beautify statement: " + input);
        }
    }
}
