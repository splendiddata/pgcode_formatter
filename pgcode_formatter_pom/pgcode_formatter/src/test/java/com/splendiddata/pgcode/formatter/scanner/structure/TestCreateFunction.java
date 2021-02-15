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
 * JUnit tests for {@link com.splendiddata.pgcode.formatter.scanner.structure.CreateFunctionNode}
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestCreateFunction {

    private static String[][] testCases() {
        return new String[][] { {
                // @formatter:off
                // source, empty declare section
                "CREATE OR REPLACE FUNCTION public.empty_declare_section()\n" +
                        "RETURNS integer\n" +
                        "LANGUAGE plpgsql\n" +
                        "AS $function$\n" +
                        "DECLARE\n" +
                        "    \n" +
                        "BEGIN\n" +
                        "    RETURN 1;\n" +
                        "END;\n" +
                        "$function$",
                // expected
                "CREATE OR REPLACE FUNCTION public.empty_declare_section()\n" +
                        "RETURNS integer\n" +
                        "LANGUAGE plpgsql\n" +
                        "AS $function$\n" +
                        "DECLARE\n" +
                        "BEGIN\n" +
                        "    RETURN 1;\n" +
                        "END;\n" +
                        "$function$"
                // @formatter:on
                },
                {
                // @formatter:off
                // source, with declaration in declare section
                "CREATE OR REPLACE FUNCTION public.empty_declare_section()\n" +
                        "RETURNS integer\n" +
                        "LANGUAGE plpgsql\n" +
                        "AS $function$\n" +
                        "DECLARE quantity integer := 30;" +
                        "    \n" +
                        "BEGIN\n" +
                        "    RETURN 1;\n" +
                        "END;\n" +
                        "$function$",
                // expected
                "CREATE OR REPLACE FUNCTION public.empty_declare_section()\n" + 
                "RETURNS integer\n" + 
                "LANGUAGE plpgsql\n" + 
                "AS $function$\n" + 
                "DECLARE\n" + 
                "    quantity                  integer := 30;\n" + 
                "BEGIN\n" + 
                "    RETURN 1;\n" + 
                "END;\n" + 
                "$function$"
                // @formatter:on
                }};
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testCreateFunctionStatement(String input, String expected) throws IOException {
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(input))) {
            CreateFunctionNode stmt = new CreateFunctionNode(reader.getFirstResult());
            Assertions.assertEquals(input, stmt.toString(), "stmt.toString()");
            FormatConfiguration config = new FormatConfiguration((Configuration) null);
            Assertions.assertEquals(expected, stmt.beautify(new FormatContext(config, null), null, config).beautify(),
                    "tried to beautify statement: " + input);
        }
    }
}
