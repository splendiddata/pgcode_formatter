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
import com.splendiddata.pgcode.formatter.scanner.structure.SrcNode;

/**
 * JUnit tests for create trigger statements
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestCreateTrigger {
    private static String[][] testCases() {
        return new String[][] {
                // @formatter:off
                {
                    // input
                    "CREATE TRIGGER tr_check\n" +
                    "    BEFORE UPDATE ON address\n" +
                    "    FOR EACH ROW\n" +
                    "    EXECUTE FUNCTION check_address_update()",
                    // expected
                    "CREATE TRIGGER tr_check BEFORE UPDATE ON address FOR EACH ROW EXECUTE FUNCTION check_address_update()"
                // @formatter:on
                },
                {
                    // input
                    "CREATE TRIGGER tr_check BEFORE UPDATE ON address FOR EACH ROW EXECUTE FUNCTION check_address_update()",
                    // expected
                    "CREATE TRIGGER tr_check BEFORE UPDATE ON address FOR EACH ROW EXECUTE FUNCTION check_address_update()"
                // @formatter:on
                },
                {
                // @formatter:off
                    // input
                    "CREATE TRIGGER tr_check\n" +
                    "    BEFORE UPDATE OF city ON address\n" +
                    "    FOR EACH ROW\n" +
                    "    EXECUTE FUNCTION check_address_update()",
                    // expected
                    "CREATE TRIGGER tr_check BEFORE UPDATE OF city ON address FOR EACH ROW EXECUTE FUNCTION check_address_update()"
                // @formatter:on
                } };
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testCreateTriggerStatement(String input, String expected) throws IOException {

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
