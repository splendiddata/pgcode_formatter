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

import java.io.StringReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.Configuration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderResult;

/**
 * Some JUnit tests for the {@link OnConflictNode}
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestOnConflictNode {
    private static final Logger log = LogManager.getLogger(TestOnConflictNode.class);

    @Test
    public void onConflictDoNothing() {
        String src = "on conflict do nothing;";
        String output = null;
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            OnConflictNode onConflict = new OnConflictNode(postgresInputReader.getFirstResult());
            RenderResult renderResult = onConflict.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
            Assertions.assertEquals("on conflict do nothing", output);
        } catch (Exception e) {
            log.error("onConflictDoNothing()", e);
            Assertions.fail(e.toString(), e);
        }
    }

    @Test
    public void onConflictDoUpdate() {
        String src = "on conflict (one_key_column, another_KEY_column, a_third_key_column, \"and even e Fourth key column\")"
                + "do update set SOME_COLUMN_WITH_A_LONG_NAME = excluded.some_column_with_a_long_name"
                + ", another_column_with_a_long_name = eXcluded.another_column_with_a_long_name;";
        String output = null;
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            OnConflictNode onConflict = new OnConflictNode(postgresInputReader.getFirstResult());
            RenderResult renderResult = onConflict.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
            Assertions.assertEquals(
                    "on conflict ( one_key_column, another_KEY_column\n" + 
                    "            , a_third_key_column\n" + 
                    "            , \"and even e Fourth key column\" )\n" + 
                    "    do update set SOME_COLUMN_WITH_A_LONG_NAME = excluded.some_column_with_a_long_name\n" + 
                    "                , another_column_with_a_long_name = eXcluded.another_column_with_a_long_name",
                    output);
        } catch (Exception e) {
            log.error("onConflictDoUpdate()", e);
            Assertions.fail(e.toString(), e);
        }
    }

    @Test
    public void onConflictOnConstraint() {
        String src = "on conflict on constraint prim_key do update set SOME_COLUMN_WITH_A_LONG_NAME = excluded.some_column_with_a_long_name"
                + ", another_column_with_a_long_name = eXcluded.another_column_with_a_long_name where prim_key > 12345";
        String output = null;
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            OnConflictNode onConflict = new OnConflictNode(postgresInputReader.getFirstResult());
            RenderResult renderResult = onConflict.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
            Assertions.assertEquals(
                    "on conflict on constraint prim_key\n" + 
                    "    do update set SOME_COLUMN_WITH_A_LONG_NAME = excluded.some_column_with_a_long_name\n" + 
                    "                , another_column_with_a_long_name = eXcluded.another_column_with_a_long_name where prim_key > 12345",
                    output);
        } catch (Exception e) {
            log.error("onConflictOnConstraint()", e);
            Assertions.fail(e.toString(), e);
        }
    }

}
