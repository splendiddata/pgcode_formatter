/*
 * Copyright (c) Splendid Data Product Development B.V. 2020 - 2021
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

package com.splendiddata.pgcode.formatter.scanner;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Some test cases for the source scanner
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestSourceScanner {
    @Test
    public void testSimpleDollarQuote() throws IOException {
        String input = "$$abc$$";
        ScanResult scanResult = new SourceScannerImpl(new StringReader(input)).scan();
        Assertions.assertNotNull(scanResult,
                "Expecting a result from new SourceScannerImpl(new StringReader(input)).scan() with input \"" + input
                        + "\"");
        Assertions.assertEquals(ScanResultType.LITERAL, scanResult.getType(),
                "Expecting type LITERAL from input \"" + input + "\"");
        Assertions.assertTrue(scanResult instanceof ScanResultStringLiteral,
                "Expecting class of result to be ScanResultStringLiteral from input \"" + input + "\", is "
                        + scanResult.getClass().getName());
        Assertions.assertEquals("$$abc$$", scanResult.toString(),
                "Expecting toString() to reproduce the input: \"" + input + "\"");
        Assertions.assertEquals("abc", scanResult.getText(),
                "Expecting getText() to reproduce the content of input: \"" + input + "\"");
        Assertions.assertEquals("$$", ((ScanResultStringLiteral) scanResult).getQuoteString(),
                "Expecting quote $$ from input: \"" + input + "\"");
    }

    @Test
    public void testMultiWordDollarQuote() throws IOException {
        String input = "$x$abc def$g$x$ $whatever$a b$whatever$";
        ScanResult scanResult = new SourceScannerImpl(new StringReader(input)).scan();
        Assertions.assertNotNull(scanResult,
                "Expecting a result from new SourceScannerImpl(new StringReader(input)).scan() with input \"" + input
                        + "\"");
        Assertions.assertEquals(ScanResultType.LITERAL, scanResult.getType(),
                "Expecting type LITERAL from input \"" + input + "\"");
        Assertions.assertTrue(scanResult instanceof ScanResultStringLiteral,
                "Expecting class of result to be ScanResultStringLiteral from input \"" + input + "\", is "
                        + scanResult.getClass().getName());
        Assertions.assertEquals("$x$abc def$g$x$", scanResult.toString(),
                "toString() as first result  from input: \"" + input + "\"");
        Assertions.assertEquals("abc def$g", scanResult.getText(),
                "Expecting getText() to reproduce the content of input: \"" + input + "\"");
        Assertions.assertEquals("$x$", ((ScanResultStringLiteral) scanResult).getQuoteString(),
                "Expecting quote $x$ from input: \"" + input + "\"");

        scanResult = scanResult.getNext();
        Assertions.assertNotNull(scanResult,
                "Expecting a whitespace node as second result from input: \"" + input + "\"");
        Assertions.assertEquals(ScanResultType.WHITESPACE, scanResult.getType(),
                "Expecting type WHITSPACE as second result from input \"" + input + "\"");

        scanResult = scanResult.getNext();
        Assertions.assertNotNull(scanResult,
                "Expecting a whitespace node as third result from input: \"" + input + "\"");
        Assertions.assertEquals(ScanResultType.LITERAL, scanResult.getType(),
                "Expecting type LITERAL as third result from input \"" + input + "\"");
        Assertions.assertTrue(scanResult instanceof ScanResultStringLiteral,
                "Expecting class of result to be ScanResultStringLiteral as third result from input \"" + input
                        + "\", is " + scanResult.getClass().getName());
        Assertions.assertEquals("$whatever$a b$whatever$", scanResult.toString(),
                "toString() as third result from input: \"" + input + "\"");
        Assertions.assertEquals("a b", scanResult.getText(),
                "getText() from third result from input: \"" + input + "\"");
        Assertions.assertEquals("$whatever$", ((ScanResultStringLiteral) scanResult).getQuoteString(),
                "Expecting quote $x$ as third result from input: \"" + input + "\"");
    }
}
