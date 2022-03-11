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

package com.splendiddata.pgcode.formatter.scanner.structure;

import java.util.LinkedList;
import java.util.List;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * The combined keywords like (order by, group by). Two keywords separated by a white space.
 */
public class CombinedIdentifierNode extends SrcNode {

    private final List<SrcNode> constituentParts = new LinkedList<>();

    /**
     * Constructor
     *
     * @param identifiers
     *            The parts of a combined identifier
     */
    public CombinedIdentifierNode(IdentifierNode... identifiers) {
        super(ScanResultType.COMBINED_IDENTIFIER, identifiers.length > 0 ? identifiers[0].getStartScanResult() : null);
        boolean first = true;
        for (IdentifierNode node : identifiers) {
            if (first) {
                first = false;
            } else {
                constituentParts.add(new WhitespaceNode(" "));
                first = false;
            }
            constituentParts.add(node);
            setNext(node.getNext());
        }

    }

    /**
     * Returns the combined keyword like (order by, group by) with white space separated.
     *
     * @see com.splendiddata.pgcode.formatter.scanner.structure.IdentifierNode#getIdentifier()
     *
     * @return String combined keyword like (order by, group by) with white space separated.
     */
    public String getIdentifier() {
        StringBuilder result = new StringBuilder();
        String separator = "";
        for (SrcNode node : constituentParts) {
            switch (node.getType()) {
            case IDENTIFIER:
                result.append(separator).append(((IdentifierNode) node).getIdentifier());
                separator = " ";
                break;
            default:
                break;
            }
        }
        return result.toString();
    }

    /**
     * @see com.splendiddata.pgcode.formatter.scanner.structure.IdentifierNode#toString()
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (SrcNode node : constituentParts) {
            result.append(node);
        }
        return result.toString();
    }

    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult, FormatConfiguration config) {
        RenderMultiLines result = new RenderMultiLines(this, formatContext, parentResult);
        String separator = "";
        for (SrcNode node : constituentParts) {
            switch (node.getType()) {
            case IDENTIFIER:
                result.addRenderResult(new RenderItem(separator, RenderItemType.WHITESPACE), formatContext);
                result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
                separator = " ";
                break;
            default:
                break;
            }
        }

        return result;

    }

}
