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

package com.splendiddata.pgcode.formatter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Walks the src/test/resources/regression/source and src/test/resources/regression/config directories and creates a
 * test case for every combination.
 * <p>
 * The output files are the target/test/regression/out directory.
 * </p>
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestIndividualCase {
    private static final Logger log = LogManager.getLogger(TestIndividualCase.class);

    private static Path basePath;

    /**
     * Tries to figure out the base path of the project. Different editors / running environments sometimes have
     * different opinions on the current directory
     */
    @BeforeAll
    static void beforeAll() {

        Object mavenBaseDir = System.getProperties().get("basedir");
        if (mavenBaseDir == null) {
            basePath = Paths.get(".").toAbsolutePath();
            /*
             * Find the directory that contains the src directory
             */
            for (basePath = Paths.get(".").toAbsolutePath().getParent(); basePath != null
                    && !Files.isDirectory(Paths.get(basePath.toString(), "src")); basePath = basePath.getParent()) {
                log.trace(() -> "finding basePath = " + basePath);
            }
        } else {
            basePath = Paths.get(mavenBaseDir.toString()).toAbsolutePath();
        }
        log.debug(() -> "basePath = " + basePath);
    }

    /**
     * Processes a combination of an input sql file with the specified configuration xml file
     * <p>
     * In fact this unit test doesn't test very much. But you can use it to check a single case. Check the output in
     * target/test/test_output.sql
     * 
     * @throws IOException
     *             when the file system feels like throwing it
     */
    @Test
    void testSqlFileWithConfig() throws IOException {
        log.info("testSqlFileWithConfig()");

        Path configFile = Paths.get(basePath.toString(), "src/main/resources/profiles/profile3.xml");
        Path outputFile = Paths.get(basePath.toString(), "target/test/test_output.sql");

                String input = new String(Files.readAllBytes(
                        Paths.get(basePath.toString(), "src/test/resources/regression/source/regtest/dollar_inside_quoted_text.sql")));

//        String input = "CREATE OR REPLACE FUNCTION plpgsql_inline_handler_test(internal)\n" + 
//                " RETURNS void\n" + 
//                " LANGUAGE c\n" + 
//                " STRICT\n" + 
//                "AS '$libdir/plpgsql' /* comment test */ , $function$plpgsql_inline_handler$function$ security definer not leakproof;";

        Files.createDirectories(outputFile.getParent());
        FormatConfiguration config = new FormatConfiguration(configFile);
        //        FormatConfiguration config = new FormatConfiguration((Configuration)null);
        String output = null;
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            output = CodeFormatter.toStringResults(new StringReader(input), config)
                    .collect(Collectors.joining());
            writer.append(output);
        }
        Assertions.assertNotNull(output, "The output is supposed to not be null");
        Assertions.assertEquals(
                "CREATE FUNCTION sales_func\n"
                + "        ( employee_id int\n"
                + "        , vehicle_id  int\n"
                + "        )\n"
                + "RETURNS sales\n"
                + "AS $$\n"
                + "DECLARE\n"
                + "    car_model   text;\n"
                + "    car_price   int;\n"
                + "    sales_bonus int;\n"
                + "    bonus       int;\n"
                + "BEGIN\n"
                + "    EXECUTE 'SELECT model, sales_bonus, price FROM cars WHERE car_id = $1' INTO car_model, sales_bonus, car_price USING vehicle_id;\n"
                + "    INSERT INTO sales (staff_id, car_id, staff_bonus, sales_price)\n"
                + "        VALUES (employee_id, vehicle_id, bonus, car_price);\n"
                + "    RETURN QUERY SELECT * FROM sales ORDER BY created_at;\n"
                + "END;\n"
                + "$$\n"
                + "LANGUAGE plpgsql;\n"
                + "", output);
    }
}
