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

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * The set clause of an update statement or the set clause of an on conflict clause of an insert statement
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class SetClauseNode extends SrcNode implements WantsNewlineBefore {

    /**
     * Constructor
     *
     * @param scanResult
     *            The word SET
     */
    public SetClauseNode(ScanResult scanResult) {
        super(ScanResultType.INTERPRETED, PostgresInputReader.toIdentifier(scanResult));
        ScanResult previousNode = getStartScanResult().locatePriorToNextInterpretable();
        ScanResult currentNode = CommaSeparatedList.withArbitraryEnd(previousNode.getNext(),
                node -> PostgresInputReader.interpretStatementBody(node), node -> {
                    if (node.is(ScanResultType.IDENTIFIER)) {
                        switch (node.toString().toLowerCase()) {
                        case "from":
                        case "where":
                        case "returning":
                            return true;
                        default:
                            break;
                        }
                    }
                    return false;
                });
        previousNode.setNext(currentNode);
        if (currentNode == null) {
            currentNode = previousNode;
        }
        setNext(currentNode.getNext());
        currentNode.setNext(null);
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderMultiLines beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        int availableWidth = formatContext.getAvailableWidth();
        RenderMultiLines result = new RenderMultiLines(this, formatContext, parentResult);
        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            result.addRenderResult(node.beautify(formatContext.setAvailableWidth(availableWidth - result.getPosition()),
                    result, config), formatContext);
        }
        formatContext.setAvailableWidth(availableWidth);
        return result;
    }

}
