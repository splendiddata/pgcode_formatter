package com.splendiddata.pgcode.formatter.scanner.structure;

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

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.Configuration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.scanner.structure.SrcNode;

/**
 * JUnit tests for create policy statements
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestCreatePolicy {
    private static String[][] testCases() {
        return new String[][] {
                // @formatter:off
                {
                        // input
                        "CREATE POLICY can_select_object ON object\n" +
                                "  FOR SELECT\n" +
                                "  USING (\n" +
                                "    can_do_the_thing(get_current_user(), owner_id)\n" +
                                "  )",
                        // expected
                        "CREATE POLICY can_select_object ON object FOR SELECT USING (can_do_the_thing(get_current_user(), owner_id))"
                        // @formatter:on
                }, {
                        // input
                        "CREATE POLICY can_insert_object ON object\n" + "  FOR INSERT\n" + "  WITH CHECK (\n"
                                + "    can_do_the_thing(get_current_user(), owner_id)\n" + "  )",
                        // expected
                        "CREATE POLICY can_insert_object ON object FOR INSERT WITH CHECK (can_do_the_thing(get_current_user(), owner_id))"
                // @formatter:on
                }, {
                // @formatter:off
                        // input
                        "CREATE POLICY can_delete_object ON object\n" +
                                "  FOR DELETE\n" +
                                "  USING (\n" +
                                "    can_do_the_thing(get_current_user(), owner_id)\n" +
                                "  )",
                        // expected
                        "CREATE POLICY can_delete_object ON object FOR DELETE USING (can_do_the_thing(get_current_user(), owner_id))"
                        // @formatter:on
                } };
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testCreatePolicyStatement(String input, String expected) throws IOException {

        StringBuilder output = new StringBuilder();
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        FormatContext context = new FormatContext(config, null);
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(input))) {
            for (SrcNode node = PostgresInputReader
                    .interpretStatementStart(reader.getFirstResult()); node != null; node = PostgresInputReader
                            .interpretStatementStart(node.getNext())) {
                output.append(node.beautify(context, null, config));
            }
        }
        Assertions.assertEquals(expected, output.toString(), "tried to beautify statement: " + input);
    }
}
