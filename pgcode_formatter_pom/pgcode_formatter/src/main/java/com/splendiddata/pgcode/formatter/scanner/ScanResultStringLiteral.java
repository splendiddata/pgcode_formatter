/*
 * Copyright (c) Splendid Data Product Development B.V. 2020
 *
 * This program is free software: You may redistribute and/or modify under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or (at Client's option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, Client should
 * obtain one via www.gnu.org/licenses/.
 */

package com.splendiddata.pgcode.formatter.scanner;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.internal.Util;

/**
 * Result from the scanner in case of a String literal; single/double quoted string or dollar quoted string.
 */
public class ScanResultStringLiteral extends ScanResultImpl {
    private final String quoteString;
    private int singleLineLength = 0;
    private RenderResult renderResult;

    /**
     * Constructor
     * 
     * @param type
     *            A {@link ScanResultType}
     * @param text
     *            A text as a String
     * @param quoteString
     *            Quoted String
     * @param scanner
     *            The scanner that delivered this scan result
     */
    public ScanResultStringLiteral(ScanResultType type, String text, String quoteString, SourceScanner scanner) {
        super(type, text, scanner);
        this.quoteString = quoteString;
    }

    /**
     * @return Quoted String
     */
    public String getQuoteString() {
        return quoteString;
    }

    @Override
    public String toString() {
        return quoteString + getText() + quoteString;
    }

    /**
     * @see ScanResult#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        if (renderResult != null) {
            return renderResult.clone();
        }
        if (getSingleLineWidth(config) >= 0) {
            renderResult = new RenderItem(toString(), RenderItemType.LITERAL);
        } else {
            renderResult = new RenderMultiLines(this, formatContext, parentResult);
            boolean first = true;
            for (String line : Util.NEWLINE_PATTERN.split(toString())) {
                if (first) {
                    first = false;
                } else {
                    ((RenderMultiLines) renderResult).addLine("");
                }
                ((RenderMultiLines) renderResult).addRenderResult(new RenderItem(line, RenderItemType.LITERAL),
                        formatContext);
            }
        }
        return renderResult.clone();
    }

    /**
     * @see ScanResult#getSingleLineWidth(FormatConfiguration)
     */
    @Override
    public int getSingleLineWidth(FormatConfiguration config) {
        if (singleLineLength == 0) {
            if (getText().contains("\n")) {
                singleLineLength = -1;
            } else {
                singleLineLength = getText().length() + 2 * quoteString.length();
            }
        }
        return singleLineLength;
    }

}
