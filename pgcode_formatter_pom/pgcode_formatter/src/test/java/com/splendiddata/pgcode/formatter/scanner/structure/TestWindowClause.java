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
import org.junit.jupiter.api.Test;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.Configuration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.scanner.structure.WindowClause;

/**
 * Some test cases for the window clause of a select statement
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestWindowClause {
    @Test
    public void simpleWindowClause() throws IOException {
        String src = "window a as (range between 1 preceding and current row)";
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(src))) {
            WindowClause windowClause = new WindowClause(reader.getFirstResult());
            Assertions.assertEquals(src, windowClause.toString(), "windowClause.toString()");
            FormatConfiguration config = new FormatConfiguration((Configuration) null);
            Assertions.assertEquals(src, windowClause.beautify(new FormatContext(config, null), null, config).beautify(),
                    "beautify");
        }
    }

    @Test
    public void twoWindowDefinitions() throws IOException {
        String src = "window window_a as (range between 1 preceding and curent row), window_b as (range between current row and unbounded following row)";
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(src))) {
            WindowClause windowClause = new WindowClause(reader.getFirstResult());
            Assertions.assertEquals(src, windowClause.toString(), "windowClause.toString()");
            FormatConfiguration config = new FormatConfiguration((Configuration) null);
            Assertions.assertEquals(
                    "window window_a as (range between 1 preceding and curent row)\n" + 
                    "     , window_b as (range between current row and unbounded following row)",
                    windowClause.beautify(new FormatContext(config, null), null, config).beautify(), "beautify");
        }
    }
}
