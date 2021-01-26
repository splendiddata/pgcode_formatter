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
import com.splendiddata.pgcode.formatter.scanner.structure.PlpgsqlIfStatement;

/**
 * JUnit tests for PlpgsqlIfStatement
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestPlpgsqlIfStatement {
    @Test
    public void justAnIfStatement() throws IOException {
        String src = "if a = b then raise notice 'a and b are equal'; else raise notice 'a and b are not equal'; end if;";
        String output = null;
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            PlpgsqlIfStatement stmt = new PlpgsqlIfStatement(postgresInputReader.getFirstResult());
            RenderResult renderResult = stmt.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
            Assertions.assertEquals("if a = b then\n" + "    raise notice 'a and b are equal';\n" + "else\n"
                    + "    raise notice 'a and b are not equal';\n" + "end if;", output);
        }
    }

    @Test
    public void moreComplexContent() throws IOException {
        String src = "if (SELECT (CASE count(*) WHEN 0 then FALSE else TRUE END) FROM tbl_country2012_l1 WHERE arrIx = v_key) THEN\n"
                + "        RAISE NOTICE 'f_AAString.....|***(Severe) KEY Jimi MUST NOT EXIST.';\n"
                + "        v_ret := 0;\n" + "    ELSE\n"
                + "        RAISE NOTICE 'f_AAString.....| good because key Jimi does NOT EXIST';\n" + "    END IF;";

        String output = null;
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            PlpgsqlIfStatement stmt = new PlpgsqlIfStatement(postgresInputReader.getFirstResult());
            RenderResult renderResult = stmt.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
            Assertions.assertEquals("if ( SELECT ( CASE count(*) WHEN 0 then FALSE\n" + 
                    "              else TRUE\n" + 
                    "              END )\n" + 
                    "     FROM tbl_country2012_l1\n" + 
                    "     WHERE arrIx = v_key )\n" + 
                    "THEN\n" + 
                    "    RAISE NOTICE 'f_AAString.....|***(Severe) KEY Jimi MUST NOT EXIST.';\n" + 
                    "    v_ret := 0;\n" + 
                    "ELSE\n" + 
                    "    RAISE NOTICE 'f_AAString.....| good because key Jimi does NOT EXIST';\n" + 
                    "END IF;",
                    output);
        }
    }
}
