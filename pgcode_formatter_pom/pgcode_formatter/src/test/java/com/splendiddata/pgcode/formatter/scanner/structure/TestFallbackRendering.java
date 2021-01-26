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
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.Configuration;

/**
 * Tests if something that is not understood still renders correctly and maybe even somewhat formatted
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestFallbackRendering {

    @Test
    public void smallUnknownStatements() throws IOException {
        String input = "statement_one a; /* some comment */ statement_two f(a,b) with some extra code;";
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        String output = com.splendiddata.pgcode.formatter.CodeFormatter
                .toStringResults(new StringReader(input), config).collect(Collectors.joining());
        Assertions.assertEquals(
                "statement_one\na; /* some comment */\nstatement_two\nf(a, b)\nwith some extra code;\n",
                output.toString(), "input: " + input);
    }
}
