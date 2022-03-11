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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.Configuration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;

/**
 * JUnit tests for {@link com.splendiddata.pgcode.formatter.scanner.structure.AnonymousCodeBlockNode}
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestAnonymousCodeBlockNode {

    private static String[][] testCases() {
        return new String[][] { {
                // @formatter:off
                // input
                "DO $$ DECLARE BEGIN END $$",
                // expected
                "DO $$\n" + 
                "DECLARE\n" + 
                "BEGIN\n" + 
                "END\n" + 
                "$$"
                // @formatter:on
                }, {
                // @formatter:off
                //input
                "DO $$ DECLARE counter integer := 0; BEGIN counter := counter + 1; END $$",
                // expected
                "DO $$\n" + 
                "DECLARE\n" + 
                "    counter                   integer := 0;\n" + 
                "BEGIN\n" + 
                "    counter := counter + 1;\n" + 
                "END\n" + 
                "$$" } };
                // @formatter:on
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testAnonymousCodeBlock(String input, String expected) throws IOException {
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(input))) {
            AnonymousCodeBlockNode stmt = new AnonymousCodeBlockNode(reader.getFirstResult());
            Assertions.assertEquals(input, stmt.toString(), "stmt.toString()");
            FormatConfiguration config = new FormatConfiguration((Configuration) null);
            Assertions.assertEquals(expected, stmt.beautify(new FormatContext(config, null), null, config).beautify(),
                    "tried to beautify statement: " + input);
        }
    }

    @Test
    public void testDoBlockAndSemicolon() throws IOException {
        String sql =
        // @formatter:off
                "DO $$\n" +
                "    DECLARE\n" +
                "    BEGIN\n" +
                "        \n" +
                "    END\n" +
                "$$;";
        // @formatter:on

        String expected =
        // @formatter:off
                "DO $$\n" + 
                "DECLARE\n" + 
                "BEGIN\n" + 
                "END\n" + 
                "$$";
        // @formatter:on
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(sql))) {
            AnonymousCodeBlockNode stmt = new AnonymousCodeBlockNode(reader.getFirstResult());
            FormatConfiguration config = new FormatConfiguration((Configuration) null);
            Assertions.assertEquals(expected, stmt.beautify(new FormatContext(config, null), null, config).beautify(),
                    "tried to beautify statement: " + sql);
        }
    }
}
