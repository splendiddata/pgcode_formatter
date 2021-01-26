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
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.LetterCaseType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.ObjectFactory;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.structure.IntoClauseNode;

/**
 * Tests the IntoClauseNode
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestIntoClauseNode {
    @Test
    public void justInto() throws IOException {
        String src = "into tbl";
        String output = null;
        FormatConfiguration config = new FormatConfiguration((Configuration)null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            IntoClauseNode into = new IntoClauseNode(postgresInputReader.getFirstResult());
            RenderResult renderResult = into.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
            Assertions.assertEquals("into tbl", output);
        }
    }
    
    @Test
    public void intoTable() throws IOException {
        String src = "into table tbl where";
        String output = null;
        FormatConfiguration config = new FormatConfiguration(new ObjectFactory().createConfiguration());
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            IntoClauseNode into = new IntoClauseNode(postgresInputReader.getFirstResult());
            RenderResult renderResult = into.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
            Assertions.assertEquals("into table tbl", output);
        }
    }
    
    @Test
    public void intoTempTable() throws IOException {
        String src = "into temp table table where";
        String output = null;
        Configuration cfg = new ObjectFactory().createConfiguration();
        cfg.setLetterCaseKeywords(LetterCaseType.UPPERCASE);
        FormatConfiguration config = new FormatConfiguration(cfg);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            IntoClauseNode into = new IntoClauseNode(postgresInputReader.getFirstResult());
            RenderResult renderResult = into.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
            Assertions.assertEquals("INTO TEMP TABLE table", output);
        }
    }
    
    @Test
    public void intoStrict() throws IOException {
        String src = "into strict table with something following";
        String output = null;
        Configuration cfg = new ObjectFactory().createConfiguration();
        cfg.setLetterCaseKeywords(LetterCaseType.UPPERCASE);
        FormatConfiguration config = new FormatConfiguration(cfg);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            IntoClauseNode into = new IntoClauseNode(postgresInputReader.getFirstResult());
            RenderResult renderResult = into.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
            Assertions.assertEquals("INTO STRICT table", output);
        }
    }
    
    @Test
    public void notFollowedByIndentifier() throws IOException {
        String src = "into + a where";
        String output = null;
        FormatConfiguration config = new FormatConfiguration((Configuration)null);
        try (PostgresInputReader postgresInputReader = new PostgresInputReader(new StringReader(src))) {
            IntoClauseNode into = new IntoClauseNode(postgresInputReader.getFirstResult());
            RenderResult renderResult = into.beautify(new FormatContext(config, null), null, config);
            output = renderResult.beautify();
            Assertions.assertEquals("into ", output);
        }
    }
}
