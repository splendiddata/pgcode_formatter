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
import org.junit.jupiter.params.ParameterizedTest;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.Configuration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;

/**
 * Some JUnit tests operations/operators and other overlapping characters like "*" and "."
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestOperationsOverlapping {

    private static String[][] testCases() {
        return new String[][] {
                // @formatter:off
                { "SELECT name, test(employee.*) FROM employee", "SELECT name, test(employee.*) FROM employee" },
                { "select count(*) from pg_stat_activity", "select count(*) from pg_stat_activity" },
                { "SELECT COUNT(*) = 0 AS haspriv FROM pg_stat_activity", "SELECT COUNT(*) = 0 AS haspriv FROM pg_stat_activity" },
                { "select *, 2*2 as result from pg_am", "select *, 2*2 as result from pg_am" },
                { "select am.*, 2*2 as result from pg_am am", "select am.*, 2*2 as result from pg_am am" },
                { "select am.* am_result, 2*2 as more from pg_am am", "select am.* am_result, 2*2 as more from pg_am am" },
                { "select am.* am_result, 2.*2 as more from pg_am am", "select am.* am_result, 2.*2 as more from pg_am am" },
                { "select am.* am_result, 2.*.2 as more from pg_am am", "select am.* am_result, 2.*.2 as more from pg_am am" },
                { "select ARRAY[1,4,3] @> ARRAY[3,1,3]", "select ARRAY[1, 4, 3] @> ARRAY[3, 1, 3]"},
                { "select ARRAY[1,2,3] || ARRAY[4,5,6]", "select ARRAY[1, 2, 3] || ARRAY[4, 5, 6]"},
                { "select ARRAY[1.1,2.1,3.1]::int[] = ARRAY[1,2,3]", "select ARRAY[1.1, 2.1, 3.1]::int[] = ARRAY[1, 2, 3]"},
                { "select v||'a', case when v||'a' = 'aa' then 1 else 0 end, count(*) from unnest(array['a','b']) u(v) group by v||'a' order by 1",
                  // expected
                  "select v||'a', case when v||'a' = 'aa' then 1 else 0 end\n"
                  + "     , count(*)\n"
                  + "from unnest(array['a', 'b']) u(v)\n"
                  + "group by v||'a'\n"
                  + "order by 1" }
                // @formatter:on
        };
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("testCases")
    public void testOperations(String src, String expectedOutput) throws IOException {
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(src))) {
            SelectStatement stmt = new SelectStatement(reader.getFirstResult());
            FormatConfiguration config = new FormatConfiguration((Configuration) null);
            Assertions.assertEquals(expectedOutput, stmt.beautify(new FormatContext(config, null), null, config).beautify(),
                    "beautify on " + src);
        }
    }
}
