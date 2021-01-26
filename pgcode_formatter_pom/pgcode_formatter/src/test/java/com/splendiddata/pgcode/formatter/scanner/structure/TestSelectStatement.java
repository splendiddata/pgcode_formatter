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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.Configuration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.scanner.structure.SelectStatement;

/**
 * Some JUnit tests for a select statement
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
@Timeout(10)
public class TestSelectStatement {
    private static String[][] testCases() {
        return new String[][] { {
                // input 1
                "select 'enough text to create some considerable length to make this statement a multiline statement' from a WHERE p.proname ~ '^(version)$'\n"
                        + "  AND pg_catalog.pg_function_is_visible(p.oid) order by 1   -- comment before the semi colon \n  ;",
                // expected 1
                "select 'enough text to create some considerable length to make this statement a multiline statement'\n"
                        + "from a\n" + "WHERE p.proname ~ '^(version)$' AND pg_catalog.pg_function_is_visible(p.oid)\n"
                        + "order by 1 -- comment before the semi colon\n" + ";" },
                {
                        // input 2
                        "select a, b from some_table where a > b order by 1, 2 limit 10",
                        // expected 2
                        "select a, b\n" + "from some_table\n" + "where a > b\n" + "order by 1, 2\n" + "limit 10" },
                {
                        // input 3
                        "SELECT CASE WHEN (FALSE) THEN 0 WHEN (TRUE) THEN 2 END AS dummy1 FROM my_table;",
                        //expected 3
                        "SELECT CASE WHEN (FALSE) THEN 0\n            WHEN (TRUE) THEN 2\n"
                                + "       END AS dummy1\nFROM my_table;" },
                {
                        // input 4
                        "select a, b, c, d, e, f, g, h, sum(i) from\ntbla, tblb    where a>b and c > d  group\nby a, b, c, d, e, f, g, h  \n"
                                + " having d < e , e ^ 2 < f\n order by a, b,\n c, d, e, f, g\n limit 10 offset 5 FETCH  NEXT 10  ROWS   ;",
                        // expected 4
                        "select a, b, c, d, e, f, g, h, sum(i)\n" + "from tbla, tblb\n" + "where a>b and c > d\n"
                                + "group by a, b, c, d, e, f, g, h\n" + "having d < e, e ^ 2 < f\n"
                                + "order by a, b, c, d, e, f, g\n" + "limit 10\n" + "offset 5\n"
                                + "FETCH NEXT 10 ROWS;" },
                { "select * from a,b for update no wait     ;", "select * from a, b for update no wait;" },
                { "select", "select" },
                { "select;", "select;" }

        };
    }

    private static String[][] testCasesBeautifiedOnly() {
        return new String[][] {
                // @formatter:off
                {
                        // input
                        "select employee.name AS col1, max(employee.salary) as amount\n" +
                                "from\n" +
                                "      (select department.a, cast (department.column2 AS integer) as column2,\n" +
                                "         department.column3 as column3\n" +
                                "       from\n" +
                                "          department\n" +
                                "      ) as dep\n" +
                                "where employee.salary < 2000 or ( employee.salary = 5000\n" +
                                "        and employee.salary = 6000 )\n" +
                                "group by employee.name, employee.salary;",

                        // expected
                        "select employee.name AS col1\n" + 
                        "     , max(employee.salary) as amount\n" + 
                        "from ( select department.a\n" + 
                        "            , cast (department.column2 AS integer) as column2\n" + 
                        "            , department.column3 as column3\n" + 
                        "       from department ) as dep\n" + 
                        "where employee.salary < 2000 or (employee.salary = 5000 and employee.salary = 6000)\n" + 
                        "group by employee.name, employee.salary;"
                },
                {
                        // input
                        "select employee.name AS col1,max(employee.salary) as amount\n" +
                                "from employee where dep in ( select" +
                                "    department.depname as dep from department)",

                        // expected
                        "select employee.name AS col1\n" + 
                        "     , max(employee.salary) as amount\n" + 
                        "from employee\n" + 
                        "where dep in (select department.depname as dep from department)"
                }
                // @formatter:on
        };
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("testCases")
    public void testSelectStatement(String src, String expectedOutput) throws IOException {
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(src))) {
            SelectStatement stmt = new SelectStatement(reader.getFirstResult());
            Assertions.assertEquals(src, stmt.toString(), "stmt.toString()");
            FormatConfiguration config = new FormatConfiguration((Configuration) null);
            Assertions.assertEquals(expectedOutput,
                    stmt.beautify(new FormatContext(config, null), null, config).beautify(), "beautify on " + src);
        }
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("testCasesBeautifiedOnly")
    public void testSelectStatementBeautified(String src, String expectedOutput) throws IOException {
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(src))) {
            SelectStatement stmt = new SelectStatement(reader.getFirstResult());
            FormatConfiguration config = new FormatConfiguration((Configuration) null);
            Assertions.assertEquals(expectedOutput,
                    stmt.beautify(new FormatContext(config, null), null, config).beautify(), "beautify on " + src);
        }
    }
}
