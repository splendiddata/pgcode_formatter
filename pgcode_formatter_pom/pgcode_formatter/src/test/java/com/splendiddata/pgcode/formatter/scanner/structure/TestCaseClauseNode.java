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
 * JUnit tests for {@link CaseClauseNode}
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */

public class TestCaseClauseNode {
    private static String[][] testCases() {
        return new String[][] { {
                // @formatter:off
                // input
                "CASE WHEN true THEN 1 END",
                // expected
                "CASE WHEN true THEN 1 END"
                }, {
                    // @formatter:off
                    // input
                    "CASE WHEN true THEN 1 else 2 END",
                    // expected
                    "CASE WHEN true THEN 1 else 2 END"
                    },
                { "CASE", "CASE" }
                // @formatter:on
        };
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testCaseStatements(String input, String expected) throws IOException {
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(input))) {
            CaseClauseNode stmt = new CaseClauseNode(reader.getFirstResult());
            Assertions.assertEquals(input, stmt.toString(), "stmt.toString()");
            FormatConfiguration config = new FormatConfiguration((Configuration) null);
            Assertions.assertEquals(expected, stmt.beautify(new FormatContext(config, null), null, config).beautify(),
                    "tried to beautify statement: " + input);
        }
    }

}
