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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.Util;

/**
 * JUnit tests for {@link com.splendiddata.pgcode.formatter.internal.RenderMultiLines}
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TestRenderMultiLines {
    private static FormatContext ctx;
    private static String indentation = "    ";

    private 
    @BeforeAll
    static void setUp() {
        // Just because it is needed when calling some methods
        ctx = new FormatContext(null, null);
    }
    
    @Test
    public void test() {
        Pattern p = Pattern.compile("^([^\\n]*)\\n(.*?)([^\\n]*)$", Pattern.DOTALL);
        String text = " a b ";
        Matcher m = p.matcher(text);
        if (m.matches()) {
            Assertions.fail("Unexpected match");
        } else {
            Assertions.assertEquals(-1, text.indexOf('\n'));
        }

        text = "a\nb";
        m = p.matcher(text);
        if (m.matches()) {
            Assertions.assertEquals(m.group(1), "a");
            Assertions.assertEquals(m.group(2), "");
            Assertions.assertEquals(m.group(3), "b");
        } else {
            Assertions.assertEquals(-1, text.indexOf('\n'));
        }

        text = "a\nb\nc";
        m = p.matcher(text);
        if (m.matches()) {
            Assertions.assertEquals(m.group(1), "a");
            Assertions.assertEquals(m.group(2), "b\n");
            Assertions.assertEquals(m.group(3), "c");
        } else {
            Assertions.assertEquals(-1, text.indexOf('\n'));
        }
        text = "a\nb\nc\n";
        m = p.matcher(text);
        if (m.matches()) {
            Assertions.assertEquals(m.group(1), "a");
            Assertions.assertEquals(m.group(2), "b\nc\n");
            Assertions.assertEquals(m.group(3), "");
        } else {
            Assertions.assertEquals(-1, text.indexOf('\n'));
        }
        
        Assertions.assertTrue("".isBlank());
    }
}
