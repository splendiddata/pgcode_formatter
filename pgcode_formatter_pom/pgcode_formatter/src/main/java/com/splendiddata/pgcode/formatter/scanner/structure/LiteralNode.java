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

package com.splendiddata.pgcode.formatter.scanner.structure;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.internal.Util;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Just a character sequence with no meaning in the conversion context
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class LiteralNode extends SrcNode {
    private final String literal;
    private int singleLineLenght = 0;
    private RenderResult renderResult;

    /**
     * Constructor
     *
     * @param scanResult
     *            Source for the literal node
     */
    public LiteralNode(ScanResult scanResult) {
        super(ScanResultType.LITERAL, scanResult);
        this.literal = scanResult.getText();
        scanResult.setNext(null);
    }

    /**
     * @return String the literal
     */
    public final String getLiteral() {
        return literal;
    }

    @Override
    public String toString() {
        return getStartScanResult().toString();
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        if (renderResult != null) {
            return renderResult.clone();
        }
        if (getSingleLineWidth(config) >= 0) {
            renderResult = new RenderItem(toString(), this, RenderItemType.LITERAL);
        } else {
            renderResult = new RenderMultiLines(this, formatContext, parentResult);
            boolean first = true;
            for (String line : Util.NEWLINE_PATTERN.split(getStartScanResult().toString())) {
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
        if (singleLineLenght == 0) {
            if (literal.contains("\n")) {
                singleLineLenght = -1;
            } else {
                singleLineLenght = toString().length();
            }
        }
        return singleLineLenght;
    }

}