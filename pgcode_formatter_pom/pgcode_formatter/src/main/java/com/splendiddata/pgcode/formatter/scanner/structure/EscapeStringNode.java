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

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * An escape string constant which is specified by writing the letter E (upper or lower case)
 * just before the opening single quote, e.g., E'foo'
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class EscapeStringNode extends SrcNode {
    private final String literal;

    /**
     * Constructor
     *
     * @param scanResult
     *            Source for the escape string node
     */
    public EscapeStringNode(ScanResult scanResult) {
        super(ScanResultType.ESCAPE_STRING, scanResult);
        this.literal = scanResult.getText();
    }

    /**
     * @return String the literal
     */
    public final String getLiteral() {
        return literal;
    }

    @Override
    public String toString() {
        return "E" + '\'' + literal + '\'';
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult, FormatConfiguration config) {
        RenderResult result = new RenderItem(toString(), this, RenderItemType.LITERAL);

        return result;
    }
}