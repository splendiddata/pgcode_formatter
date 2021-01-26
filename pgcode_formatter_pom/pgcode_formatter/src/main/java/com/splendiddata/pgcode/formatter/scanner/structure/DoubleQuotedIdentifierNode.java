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

/**
 * An identifier that was double-quoted in the source.
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class DoubleQuotedIdentifierNode extends IdentifierNode {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param scanResult The identifier in double quotes
     */
    public DoubleQuotedIdentifierNode(ScanResult scanResult) {
        super(scanResult);
    }

    @Override
    public String toString() {
        return '"' + getIdentifier() + '"';
    }

    /**
     * @see IdentifierNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult, FormatConfiguration config) {
        RenderResult result = new RenderItem(toString(), this, RenderItemType.DOUBLE_QUOTED_IDENTIFIER);

        return result;
    }
}
