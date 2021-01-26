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
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;
import com.splendiddata.pgcode.formatter.scanner.structure.DoubleQuotedIdentifierNode;
import com.splendiddata.pgcode.formatter.scanner.structure.IdentifierNode;
import com.splendiddata.pgcode.formatter.scanner.structure.QualifiedIdentifierNode;
import com.splendiddata.pgcode.formatter.scanner.structure.SrcNode;

/**
 * Test cases for the interpretation of identifiers
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestIdentifierNode {
    @Test
    public void simpleIdentifier() throws IOException {
        String src = "identifier1 \"Identifier2\" a.b a.\"B\" . c";
        try (PostgresInputReader reader = new PostgresInputReader(new StringReader(src))) {
            FormatConfiguration config = new FormatConfiguration((Configuration)null);
            FormatContext formatContext = new FormatContext(config, null);
            /*
             * identifier1
             */
            ScanResult scanResult = reader.getFirstResult();
            Assertions.assertEquals(ScanResultType.IDENTIFIER, scanResult.getType(), "ScanResult.getType() from identifier1 in: " + src);
            Assertions.assertEquals("identifier1", scanResult.toString(), "ScanResult.toString() from identifier1 in " + src);
            ScanResult next = scanResult.getNext();
            Assertions.assertNotNull(next, "scanResult.getNext() from identifier1 in " + src);
            SrcNode srcNode = PostgresInputReader.interpretIdentifier(scanResult);
            Assertions.assertEquals(ScanResultType.IDENTIFIER, srcNode.getType(), "SrcNode.getType() from identifier1 in: " + src);
            Assertions.assertEquals("identifier1", srcNode.toString(), "SrcNode.toString() from identifier1 in " + src);
            next = srcNode.getNext();
            Assertions.assertNotNull(next, "srcNode.getNext() from identifier1 in " + src);
            IdentifierNode copied = new IdentifierNode(srcNode);
            Assertions.assertEquals(ScanResultType.IDENTIFIER, copied.getType(), "copied.getType() from identifier1 in: " + src);
            Assertions.assertEquals("identifier1", copied.toString(), "copied.toString() from identifier1 in " + src);
            RenderResult renderResult = copied.beautify(formatContext, null, config);
            String output = renderResult.beautify();
            Assertions.assertEquals("identifier1", output, "beautify(formatContext, config) from identifier1 in " + src);
            next = copied.getNext();
            Assertions.assertNotNull(next, "copied.getNext() from identifier1 in " + src);
            
            /*
             * "identifier2"
             */
            scanResult = copied.getNextInterpretable();
            Assertions.assertEquals(ScanResultType.DOUBLE_QUOTED_IDENTIFIER, scanResult.getType(), "ScanResult.getType() from \"Identifier2\" in: " + src);
            Assertions.assertEquals("\"Identifier2\"", scanResult.toString(), "ScanResult.toString() from \"Identifier2\" in " + src);
            next = scanResult.getNext();
            Assertions.assertNotNull(next, "scanResult.getNext() from \"Identifier2\" in " + src);
            srcNode = PostgresInputReader.interpretIdentifier(scanResult);
            Assertions.assertEquals(ScanResultType.IDENTIFIER, srcNode.getType(), "SrcNode.getType() from \"Identifier2\" in: " + src);
            Assertions.assertEquals("\"Identifier2\"", srcNode.toString(), "SrcNode.toString() from \"Identifier2\" in " + src);
            next = srcNode.getNext();
            Assertions.assertNotNull(next, "srcNode.getNext() from \"Identifier2\" in " + src);
            copied = new DoubleQuotedIdentifierNode(srcNode);
            Assertions.assertEquals(ScanResultType.IDENTIFIER, copied.getType(), "copied.getType() from \"Identifier2\" in: " + src);
            Assertions.assertEquals("\"Identifier2\"", copied.toString(), "copied.toString() from \"Identifier2\" in " + src);
             renderResult = copied.beautify(formatContext, null, config);
             output = renderResult.beautify();
            Assertions.assertEquals("\"Identifier2\"", output, "beautify(formatContext, config) from \"Identifier2\" in " + src);
            next = copied.getNext();
            Assertions.assertNotNull(next, "copied.getNext() from \"Identifier2\" in " + src);
            
            /*
             * a.b
             */
            scanResult = copied.getNextInterpretable();
            Assertions.assertEquals(ScanResultType.IDENTIFIER, scanResult.getType(), "ScanResult.getType() from a.b in: " + src);
            Assertions.assertEquals("a", scanResult.toString(), "ScanResult.toString() from a.b in " + src);
            next = scanResult.getNext();
            Assertions.assertNotNull(next, "copied.getNext() from a.b in " + src);
            Assertions.assertEquals(".",  next.toString(), "next from ScanResult.getnext() from a.b in " + src);
            srcNode = PostgresInputReader.interpretIdentifier(scanResult);
            Assertions.assertEquals(ScanResultType.IDENTIFIER, srcNode.getType(), "SrcNode.getType() from a.b in: " + src);
            Assertions.assertEquals("a.b", srcNode.toString(), "SrcNode.toString() from a.b in " + src);
            next = srcNode.getNext();
            Assertions.assertNotNull(next, "srcNode.getNext() from a.b in " + src);
            copied = new QualifiedIdentifierNode((IdentifierNode)srcNode);
            Assertions.assertEquals(ScanResultType.IDENTIFIER, copied.getType(), "copied.getType() from a.b in: " + src);
            Assertions.assertEquals("a.b", copied.toString(), "copied.toString() from a.b in " + src);
            renderResult = copied.beautify(formatContext, null, config);
            output = renderResult.beautify();
            Assertions.assertEquals("a.b", output, "beautify(formatContext, config) from a.b in " + src);
            next = copied.getNext();
            Assertions.assertNotNull(next, "copied.getNext() from a.b in " + src);
            
            /*
             * a."B" . c
             */
            scanResult = copied.getNextInterpretable();
            Assertions.assertEquals(ScanResultType.IDENTIFIER, scanResult.getType(), "ScanResult.getType() from a.\"B\" . c in: " + src);
            Assertions.assertEquals("a", scanResult.toString(), "ScanResult.toString() from a.\"B\" . c in " + src);
            next = scanResult.getNext();
            Assertions.assertNotNull(next, "copied.getNext() from a.\"B\" . c in " + src);
            Assertions.assertEquals(".",  next.toString(), "next from ScanResult.getnext() from a.\"B\" . c in " + src);
            srcNode = PostgresInputReader.interpretIdentifier(scanResult);
            Assertions.assertEquals(ScanResultType.IDENTIFIER, srcNode.getType(), "SrcNode.getType() from a.\"B\" . c in: " + src);
            Assertions.assertEquals("a.\"B\" . c", srcNode.toString(), "SrcNode.toString() from a.\"B\" . c in " + src);
            next = srcNode.getNext();
            Assertions.assertNotNull(next, "srcNode.getNext() from a.\"B\" . c in " + src);
            copied = new QualifiedIdentifierNode((IdentifierNode)srcNode);
            Assertions.assertEquals(ScanResultType.IDENTIFIER, copied.getType(), "copied.getType() from a.\"B\" . c in: " + src);
            Assertions.assertEquals("a.\"B\" . c", copied.toString(), "copied.toString() from a.\"B\" . c in " + src);
            renderResult = copied.beautify(formatContext, null, config);
            output = renderResult.beautify();
            Assertions.assertEquals("a.\"B\".c", output, "beautify(formatContext, config) from a.\"B\" . c in " + src);
            next = copied.getNext();
            Assertions.assertNotNull(next, "copied.getNext() from a.\"B\" . c in " + src);
            Assertions.assertEquals(ScanResultType.EOF, next.getType(), "getType() from copied.getNext() from a.\"B\" . c in " + src);
        }
    }
}
