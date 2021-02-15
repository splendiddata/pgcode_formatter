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
import com.splendiddata.pgcode.formatter.internal.RenderResult;

/**
 * Test the WithStatement
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestWithStatement {
    @Test
    public void simplestSingleLine() throws IOException {
        String src = "with a as (select 'x') select * from a";
        String output = null;
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            WithStatement st = new WithStatement(postgresInputReader.getFirstResult());
            RenderResult renderResult = st.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
            Assertions.assertEquals("with a as (select 'x') select * from a", output);
        }
    }

    @Test
    public void simplesMultiLine() throws IOException {
        String src = "with a as (select 'a long text to make the line too long' union all "
                + "values ('another long text, just to be sure that the line will be too long')) select * from a";
        String output = null;
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            WithStatement st = new WithStatement(postgresInputReader.getFirstResult());
            RenderResult renderResult = st.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
            //@Formatter:off
            Assertions.assertEquals("with a as ( select 'a long text to make the line too long'\n"
                    + "            union all\n"
                    + "            values ('another long text, just to be sure that the line will be too long') )\n"
                    + "select * from a", output);
            //@Formatter:on
        }
    }

}
