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

/**
 * JUnit tests for {@link com.splendiddata.pgcode.formatter.scanner.structure.UpdateTableNode}
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestUpdateTableNode {

    private static String[][] testCases() {
        return new String[][] { {
            // @formatter:off
            // input, select statement in a where clause
            "update\n" +
            "    product_version\n" +
            "    set product_version_active = false \n" +
            "where\n" +
            "    product_id = (select\n" +
            "                     product_id\n" +
            "                     from product\n" +
            "                     where product_name = lower(p_product_name)\n" +
            "                 )",
            // expected
            "update product_version\n"
            + "set product_version_active = false\n"
            + "where product_id = ( select product_id\n"
            + "                     from product\n"
            + "                     where product_name = lower(p_product_name) )"}
            // @formatter:on
        };
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testUpdateStatement(String input, String expected) throws IOException {
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(input))) {
            UpdateTableNode stmt = new UpdateTableNode(reader.getFirstResult(), true);
            Assertions.assertEquals(input, stmt.toString(), "stmt.toString()");
            FormatConfiguration config = new FormatConfiguration((Configuration) null);
            Assertions.assertEquals(expected, stmt.beautify(new FormatContext(config, null), null, config).beautify(),
                    "tried to beautify statement: " + input);
        }
    }
}
