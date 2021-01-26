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
import com.splendiddata.pgcode.formatter.scanner.structure.PlpgsqlLabel;
import com.splendiddata.pgcode.formatter.scanner.structure.SrcNode;

/**
 * A few test cases for PlpgsqlLabel
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestPlpgsqlLabel {

    @Test
    public void simpleLabel() throws IOException {
        String src = "<<aLabel>>";
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            SrcNode scanResult = PostgresInputReader
                    .interpretPlpgsqlStatementStart(postgresInputReader.getFirstResult());
            Assertions.assertTrue(scanResult instanceof PlpgsqlLabel, "scanResult instanceof PlpgsqlLabel");
            Assertions.assertEquals(src, scanResult.toString(), "scanResult.toString()");
            Assertions.assertEquals(src, scanResult.getText(), "scanResult.getText()");
            Assertions.assertEquals(src, scanResult.beautify(new FormatContext(config, null), null, config).beautify(),
                    "scanResult.beautify(new FormatContext(config, null), config)");
            Assertions.assertTrue(scanResult == PlpgsqlLabel.from(scanResult),
                    "scanResult == PlpgsqlLabel.from(scanResult)");
        }
    }

    @Test
    public void doubleQuotedIdentifierLabel() throws IOException {
        String src = "<< \"a Label\" >>";
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            SrcNode scanResult = PostgresInputReader
                    .interpretPlpgsqlStatementStart(postgresInputReader.getFirstResult());
            Assertions.assertTrue(scanResult instanceof PlpgsqlLabel, "scanResult instanceof PlpgsqlLabel");
            Assertions.assertEquals(src, scanResult.toString(), "scanResult.toString()");
            Assertions.assertEquals(src, scanResult.getText(), "scanResult.getText()");
            Assertions.assertEquals(src, scanResult.beautify(new FormatContext(config, null), null, config).beautify(),
                    "scanResult.beautify(new FormatContext(config, null), config)");
            Assertions.assertTrue(scanResult == PlpgsqlLabel.from(scanResult),
                    "scanResult == PlpgsqlLabel.from(scanResult)");
        }
    }

    @Test
    public void withCommentLabel() throws IOException {
        String src = "<< /* just some comment */ \"a Label\" -- endofline comment\n>>";
        FormatConfiguration config = new FormatConfiguration((Configuration) null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            SrcNode scanResult = PostgresInputReader
                    .interpretPlpgsqlStatementStart(postgresInputReader.getFirstResult());
            Assertions.assertTrue(scanResult instanceof PlpgsqlLabel, "scanResult instanceof PlpgsqlLabel");
            Assertions.assertEquals(src, scanResult.toString(), "scanResult.toString()");
            Assertions.assertEquals(src, scanResult.getText(), "scanResult.getText()");
            Assertions.assertEquals("<< /* just some comment */ \"a Label\" -- endofline comment\n" + 
                    "                                     >>",
                    scanResult.beautify(new FormatContext(config, null), null, config).beautify(),
                    "scanResult.beautify(new FormatContext(config, null), config)");
            Assertions.assertTrue(scanResult == PlpgsqlLabel.from(scanResult),
                    "scanResult == PlpgsqlLabel.from(scanResult)");
        }
    }

    @Test
    public void notALabel1() throws IOException {
        String src = "<aLabel>>";
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            SrcNode scanResult = PostgresInputReader
                    .interpretPlpgsqlStatementStart(postgresInputReader.getFirstResult());
            Assertions.assertFalse(scanResult instanceof PlpgsqlLabel,
                    "scanResult instanceof PlpgsqlLabel from " + src);
        }
    }

    @Test
    public void notALabel2() throws IOException {
        String src = "<<aLabel>";
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            SrcNode scanResult = PostgresInputReader
                    .interpretPlpgsqlStatementStart(postgresInputReader.getFirstResult());
            Assertions.assertFalse(scanResult instanceof PlpgsqlLabel,
                    "scanResult instanceof PlpgsqlLabel from " + src);
        }
    }

    @Test
    public void notALabel3() throws IOException {
        String src = "<<a Label >>";
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            SrcNode scanResult = PostgresInputReader
                    .interpretPlpgsqlStatementStart(postgresInputReader.getFirstResult());
            Assertions.assertFalse(scanResult instanceof PlpgsqlLabel,
                    "scanResult instanceof PlpgsqlLabel from " + src);
        }
    }

    @Test
    public void notALabel4() throws IOException {
        String src = "<< a.Label>>";
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            SrcNode scanResult = PostgresInputReader
                    .interpretPlpgsqlStatementStart(postgresInputReader.getFirstResult());
            Assertions.assertFalse(scanResult instanceof PlpgsqlLabel,
                    "scanResult instanceof PlpgsqlLabel from " + src);
        }
    }

    @Test
    public void notALabel5() throws IOException {
        String src = "<[aLabel>>";
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            SrcNode scanResult = PostgresInputReader
                    .interpretPlpgsqlStatementStart(postgresInputReader.getFirstResult());
            Assertions.assertFalse(scanResult instanceof PlpgsqlLabel,
                    "scanResult instanceof PlpgsqlLabel from " + src);
        }
    }

    @Test
    public void notALabel6() throws IOException {
        String src = "*<aLabel>>";
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            SrcNode scanResult = PostgresInputReader
                    .interpretPlpgsqlStatementStart(postgresInputReader.getFirstResult());
            Assertions.assertFalse(scanResult instanceof PlpgsqlLabel,
                    "scanResult instanceof PlpgsqlLabel from " + src);
        }
    }

    @Test
    public void notALabel7() throws IOException {
        String src = "<<aLabel]>";
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            SrcNode scanResult = PostgresInputReader
                    .interpretPlpgsqlStatementStart(postgresInputReader.getFirstResult());
            Assertions.assertFalse(scanResult instanceof PlpgsqlLabel,
                    "scanResult instanceof PlpgsqlLabel from " + src);
        }
    }

    @Test
    public void notALabel8() throws IOException {
        String src = "<<aLabel>]";
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            SrcNode scanResult = PostgresInputReader
                    .interpretPlpgsqlStatementStart(postgresInputReader.getFirstResult());
            Assertions.assertFalse(scanResult instanceof PlpgsqlLabel,
                    "scanResult instanceof PlpgsqlLabel from " + src);
        }
    }

    @Test
    public void notALabel9() throws IOException {
        String src = "a<<aLabel>>";
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            SrcNode scanResult = PostgresInputReader
                    .interpretPlpgsqlStatementStart(postgresInputReader.getFirstResult());
            Assertions.assertFalse(scanResult instanceof PlpgsqlLabel,
                    "scanResult instanceof PlpgsqlLabel from " + src);
        }
    }

    @Test
    public void notALabel10() throws IOException {
        String src = "< <aLabel>>";
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            SrcNode scanResult = PostgresInputReader
                    .interpretPlpgsqlStatementStart(postgresInputReader.getFirstResult());
            Assertions.assertFalse(scanResult instanceof PlpgsqlLabel,
                    "scanResult instanceof PlpgsqlLabel from " + src);
        }
    }
}
