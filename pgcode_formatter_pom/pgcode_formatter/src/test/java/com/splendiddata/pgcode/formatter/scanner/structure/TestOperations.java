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

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.Configuration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;

/**
 * Some JUnit tests operations/operators
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestOperations {

    private static String[][] testCases() {
        return new String[][] {
                // @formatter:off
                { "select 1+2", "select 1+2" },
                { "select a+b", "select a+b" },
                { "select a+ b", "select a+ b" },
                { "select a>= b", "select a>= b" },
                { "select 'true'::json", "select 'true'::json" },
                { "select test_json -> 'x'", "select test_json -> 'x'" },
                { "select test_json ->> 6", "select test_json ->> 6" },
                { "select '[1,2,3]'::json #> '{}'", "select '[1,2,3]'::json #> '{}'"},
                { "select '{\"a\":1, \"b\":2}'::jsonb @> '{\"b\":2}'::jsonb", "select '{\"a\":1, \"b\":2}'::jsonb @> '{\"b\":2}'::jsonb"},
                // @formatter:on
                { "select '{\"a\": [{\"b\": \"c\"}, {\"b\": \"cc\"}]}'::json -> null::text",
                        "select '{\"a\": [{\"b\": \"c\"}, {\"b\": \"cc\"}]}'::json -> null::text" },
                { "select '{\"f2\":{\"f3\":1},\"f4\":{\"f5\":99,\"f6\":\"stringy\"}}'::json#>>array['f4','f6']",
                        "select '{\"f2\":{\"f3\":1},\"f4\":{\"f5\":99,\"f6\":\"stringy\"}}'::json#>>array['f4'\n" + 
                        "     , 'f6']" },
                { "SELECT * FROM my_table ORDER BY to_date(my_data->>'date', 'YYYY-MM-DD')",
                        "SELECT *\n" + 
                        "FROM my_table\n" + 
                        "ORDER BY to_date(my_data->>'date', 'YYYY-MM-DD')" } };
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("testCases")
    public void testOperations(String src, String expectedOutput) throws IOException {
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(src))) {
            SelectStatement stmt = new SelectStatement(reader.getFirstResult());
            FormatConfiguration config = new FormatConfiguration((Configuration) null);
            Assertions.assertEquals(expectedOutput, stmt.beautify(new FormatContext(config, null), null, config).beautify(),
                    "beautify on " + src);
        }
    }
}
