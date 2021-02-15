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
 * Tests a PLpgSQl begin ... end block
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestPlpgsqlBeginEndBlock {
    @Test
    public void justABlock() throws IOException {
        String src = "begin some statement; end;";
        String output = null;
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            PlpgsqlBeginEndBlock block = new PlpgsqlBeginEndBlock(postgresInputReader.getFirstResult());
            RenderResult renderResult = block.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
            Assertions.assertEquals("begin\n    some statement;\nend;", output);
        }
    }

    @Test
    public void blockWithSimpleSelectStatmenent() throws IOException {
        String src = "begin select * from some_table; end;";
        String output = null;
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            PlpgsqlBeginEndBlock block = new PlpgsqlBeginEndBlock(postgresInputReader.getFirstResult());
            RenderResult renderResult = block.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
            Assertions.assertEquals("begin\n    select * from some_table;\nend;", output);
        }
    }

    @Test
    public void blockWithLongerSelectStatmenent() throws IOException {
        String src = "begin select 'just a bit of text to fill up a line so that we will get a multiline result',"
                + " 'and some more text with the same purpose as the first line' from some_table; end;";
        String output = null;
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            PlpgsqlBeginEndBlock block = new PlpgsqlBeginEndBlock(postgresInputReader.getFirstResult());
            RenderResult renderResult = block.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
            Assertions.assertEquals("begin\n" + 
                    "    select 'just a bit of text to fill up a line so that we will get a multiline result'\n" + 
                    "         , 'and some more text with the same purpose as the first line'\n" + 
                    "    from some_table;\n" + 
                    "end;", output);
        }
    }
}
