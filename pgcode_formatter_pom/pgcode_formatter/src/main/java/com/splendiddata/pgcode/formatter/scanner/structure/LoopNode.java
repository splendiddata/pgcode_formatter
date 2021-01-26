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
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A (simple) loop.
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class LoopNode extends SrcNode {

    /**
     * Constructor
     *
     * @param startNode
     *            The node that contains "case"
     */
    public LoopNode(ScanResult startNode) {
        super(ScanResultType.FOR_LOOP, PostgresInputReader.toIdentifier(startNode));
        assert "loop".equalsIgnoreCase(startNode.getText()) : "Expecting 'LOOP' but found: " + startNode.getText();

        ScanResult lastInterpreted = getStartScanResult();
        ScanResult priorNode = lastInterpreted.locatePriorToNextInterpretable();
        ScanResult currentNode = priorNode.getNext();
        if (currentNode != null) {
            lastInterpreted = new PlpgsqlCompoundStatementPart(currentNode);
            priorNode.setNext(lastInterpreted);
            priorNode = lastInterpreted;
        }

        for (priorNode = priorNode.locatePriorToNextInterpretable();; priorNode = priorNode
                .locatePriorToNextInterpretable()) {
            currentNode = priorNode.getNext();
            if (currentNode == null || currentNode.isStatementEnd()) {
                break;
            }
            currentNode = PostgresInputReader.interpretStatementBody(currentNode);
            if (!(currentNode.is(ScanResultType.WHITESPACE) || currentNode.is(ScanResultType.LINEFEED))) {
                lastInterpreted = currentNode;
            }
            priorNode.setNext(currentNode);
            priorNode = currentNode;
        }
        setNext(lastInterpreted.getNext());
        lastInterpreted.setNext(null);
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult, FormatConfiguration config) {
        String standardIndent = FormatContext.indent(true);
        RenderMultiLines result = new RenderMultiLines(this, formatContext).setIndent(0);
        ScanResult node = getStartScanResult();
        result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
        for (node = node.getNext(); node != null
                && !(node instanceof PlpgsqlCompoundStatementPart); node = node.getNext()) {
            if (!node.is(ScanResultType.WHITESPACE) && !node.is(ScanResultType.LINEFEED)) {
                result.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
                result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
            }
        }
        if (node != null) {
            if (result.isLastNonWhiteSpaceEqualToLinefeed()) {
                result.positionAt(standardIndent.length());
            } else {
                result.addLine(standardIndent);
            }
            result.addRenderResult(new RenderMultiLines(node, formatContext).addRenderResult(
                    node.beautify(new FormatContext(config, formatContext)
                            .setAvailableWidth(formatContext.getAvailableWidth() - standardIndent.length()), result, config),
                    formatContext), formatContext);
        }
        result.addLine();
        for (node = node.getNextNonWhitespace(); node != null; node = node.getNext()) {
            result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
        }
        return result;
    }
}
