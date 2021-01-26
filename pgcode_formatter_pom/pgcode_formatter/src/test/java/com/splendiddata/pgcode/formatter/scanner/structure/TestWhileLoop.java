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
import com.splendiddata.pgcode.formatter.scanner.ScanResult;

/**
 * Some tests for the WHILE loop
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestWhileLoop {
    @Test
    public void testWhileLoop() throws IOException {
        String src = "while ( coalesce(l_idx, '') != '' )\n" + "        loop\n"
                + "        SELECT tt.val INTO STRICT country2012_8\n" + "          FROM tbl_country2012_l1 tt\n"
                + "         WHERE arrIx = l_idx;   -- RAISE NOTICE 'f_AAString.....| k ...\n"
                + "        RAISE NOTICE 'f_AAString.....| key = % val = %', l_idx, country2012_8.val;\n"
                + "        l_idx := (SELECT arrIx FROM tbl_country2012_l1 WHERE arrIx > l_idx ORDER BY arrIx LIMIT 1);\n"
                + "        v_n := v_n + 1;\n" + "    end loop;\n" + "    --\n" + "    -- position then get prior\n"
                + "    --\n" + "    v_key := 'Toby1';";
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(src))) {
            ScanResult stmt = PostgresInputReader.interpretPlpgsqlStatementStart(reader.getFirstResult());
            Assertions.assertEquals("while ( coalesce(l_idx, '') != '' )\n" + 
                    " loop\n" + 
                    "        SELECT tt.val INTO STRICT country2012_8\n" + 
                    "          FROM tbl_country2012_l1 tt\n" + 
                    "         WHERE arrIx = l_idx; -- RAISE NOTICE 'f_AAString.....| k ...\n" + 
                    " RAISE NOTICE 'f_AAString.....| key = % val = %', l_idx, country2012_8.val;\n" + 
                    " l_idx := (SELECT arrIx FROM tbl_country2012_l1 WHERE arrIx > l_idx ORDER BY arrIx LIMIT 1);\n" + 
                    " v_n := v_n + 1;\n" + 
                    " end loop;", stmt.toString(), "toString()");

            FormatConfiguration config = new FormatConfiguration((Configuration) null);
            FormatContext formatContext = new FormatContext(config, null);
            String output = stmt.beautify(formatContext, null, config).beautify();
            Assertions.assertEquals(
                    "while (coalesce(l_idx, '') != '') loop\n" + 
                    "    SELECT tt.val\n" + 
                    "    INTO STRICT country2012_8\n" + 
                    "    FROM tbl_country2012_l1 tt\n" + 
                    "    WHERE arrIx = l_idx; -- RAISE NOTICE 'f_AAString.....| k ...\n" + 
                    "    RAISE NOTICE 'f_AAString.....| key = % val = %', l_idx, country2012_8.val;\n" + 
                    "    l_idx := ( SELECT arrIx\n" + 
                    "               FROM tbl_country2012_l1\n" + 
                    "               WHERE arrIx > l_idx\n" + 
                    "               ORDER BY arrIx\n" + 
                    "               LIMIT 1 );\n" + 
                    "    v_n := v_n + 1;\n" + 
                    "end loop;", output,
                    "tried to beautify statement: " + src);
        }
    }
}
