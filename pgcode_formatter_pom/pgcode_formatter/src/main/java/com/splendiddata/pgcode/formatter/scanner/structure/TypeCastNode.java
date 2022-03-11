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
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Represents a type cast in the form ::data_type
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TypeCastNode extends SrcNode {

    /**
     * Constructor for a ::data_type type cast
     *
     * @param scanResult
     *            Source of this node
     */
    public TypeCastNode(ScanResult scanResult) {
        super(ScanResultType.TYPE_CAST, new OperatorNode("::", scanResult));
        ScanResult priorNode = getStartScanResult().locatePriorToNextInterpretable();
        ScanResult currentNode = priorNode.getNext();
        if (currentNode == null) {
            currentNode = priorNode;
        } else {
            currentNode = PostgresInputReader.interpretStatementBody(currentNode);
            priorNode.setNext(currentNode);
        }
        setNext(currentNode.getNext());
        currentNode.setNext(null);
    }

    /**
     * @see ScanResult#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult, FormatConfiguration config) {
        RenderMultiLines result = new RenderMultiLines(this, formatContext, parentResult);
        ScanResult node;
        for (node = getStartScanResult(); node != null; node = node.getNext()) {
            if (node.is(ScanResultType.COMMENT) || node.is(ScanResultType.COMMENT_LINE)) {
                return super.beautify(formatContext, result, config);
            }
            if (node.getType().isInterpretable()) {
                if (parentResult != null) {
                    parentResult.removeTrailingSpaces();
                }
                result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
            }
        }
        return result;
    }

}
