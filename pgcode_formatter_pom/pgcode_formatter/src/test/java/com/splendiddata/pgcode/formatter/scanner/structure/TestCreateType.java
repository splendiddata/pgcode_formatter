/*
 * Copyright (c) Splendid Data Product Development B.V. 2020 - 2022
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

/**
 * JUnit tests for {@link com.splendiddata.pgcode.formatter.scanner.structure.CreateTypeNode}
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestCreateType {

    private static String[][] testCases() {
        return new String[][] { {
                // @formatter:off
                    // source
                    "CREATE TYPE",
                    // expected
                    "CREATE TYPE"
                    // @formatter:on
                }, {
                // @formatter:off
                    // source
                    "CREATE TYPE address AS (city VARCHAR(90), street VARCHAR(90))",
                    // expected
                    "CREATE TYPE address AS (city VARCHAR(90), street VARCHAR(90))"
                    // @formatter:on
                }, {
                // @formatter:off
                    // source
                    "create type status as enum ('new', 'open', 'closed')",
                    // expected
                    "create type status as enum ('new', 'open', 'closed')"
                    // @formatter:on
                }, {
                // @formatter:off
                    // source
                    "create type new_comptype_coll as (t1 int, t2 text collate \"nl_NL\")",
                    // expected
                    "create type new_comptype_coll as (t1 int, t2 text collate \"nl_NL\")"
                    // @formatter:on
                }, {
                // @formatter:off
                    // source
                    "CREATE TYPE new_type",
                    // expected
                    "CREATE TYPE new_type"
                    // @formatter:on
                }, {
                // @formatter:off
                    // source
                    "create type user_defined_type (input = input_function, output = output_function)",
                    // expected
                    "create type user_defined_type (input = input_function, output = output_function)"
                    // @formatter:on
                }, {
                // @formatter:off
                    // source
                    "create /* c1 */ type /* c2 */user_defined_type /*c3*/AS ENUM ('red', 'orange', 'yellow', 'green', 'blue')",
                    // expected
                    "create /* c1 */ type /* c2 */user_defined_type /*c3*/AS ENUM ('red', 'orange', 'yellow', 'green', 'blue')"
                    // @formatter:on
                }, {
                // @formatter:off
                    // source
                    "create type range_type as range (subtype = int4, subtype_opclass = float8_ops, " +
                    "collation = \"nl_NL\", canonical = canonical_function, subtype_diff = int4range_subdiff)",
                    // expected
                    "create type range_type as range ( subtype = int4, subtype_opclass = float8_ops\n" +
                    "                                , collation = \"nl_NL\"\n" +
                    "                                , canonical = canonical_function\n" +
                    "                                , subtype_diff = int4range_subdiff )"
                    // @formatter:on
                }, };
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testCreateTypeStatement(String input, String expected) throws IOException {
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(input))) {
            CreateTypeNode stmt = (CreateTypeNode) PostgresInputReader.interpretStatementStart(reader.getFirstResult());
            Assertions.assertEquals(input, stmt.toString(), "stmt.toString()");
            FormatConfiguration config = new FormatConfiguration((Configuration) null);
            String beautify = stmt.beautify(new FormatContext(config, null), null, config).beautify();
            Assertions.assertEquals(expected, beautify, "tried to beautify statement: " + input);
        }
    }
}
