/*
 * Copyright (c) Splendid Data Product Development B.V. 2020 - 2022
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
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A for loop.
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class ForLoopNode extends SrcNode {

    /**
     * Constructor
     *
     * @param startNode
     *            The node that contains "for"
     */
    public ForLoopNode(ScanResult startNode) {
        super(ScanResultType.FOR_LOOP, PostgresInputReader.toIdentifier(startNode));
        assert "for".equalsIgnoreCase(startNode.getText()) : "Expecting 'FOR' but found: " + startNode.getText();
        ScanResult priorNode = getStartScanResult();
        ScanResult currentNode;
        for (currentNode = priorNode.getNext(); currentNode != null
                && !currentNode.isStatementEnd(); currentNode = priorNode.getNext()) {
            if (currentNode.is(ScanResultType.IDENTIFIER) && "loop".equalsIgnoreCase(currentNode.toString())) {
                currentNode = new LoopNode(currentNode);
            } else {
                currentNode = PostgresInputReader.interpretStatementBody(currentNode);
            }
            priorNode.setNext(currentNode);
            priorNode = currentNode;
        }

        if (currentNode != null && currentNode.is(ScanResultType.SEMI_COLON)) {
            currentNode = new SemiColonNode(currentNode);
            priorNode.setNext(currentNode);
            priorNode = currentNode;
        }
        setNext(priorNode.getNext());
        priorNode.setNext(null);
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderMultiLines beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        RenderMultiLines result = getCachedRenderResult(formatContext, parentResult, config);
        if (result != null) {
            return result;
        }

        int parentPosition = 0;
        if (parentResult != null) {
            if (!parentResult.isLastNonWhiteSpaceEqualToLinefeed()) {
                parentResult.addLine();
            }
            parentPosition = parentResult.getPosition();
        }

        result = new RenderMultiLines(this, formatContext, parentResult).setIndentBase(parentPosition).setIndent(0);
        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            if (node instanceof LoopNode) {
                switch (config.getLanguagePlpgsql().getCodeSection().getForStatement().getLoop()) {
                case ON_NEW_LINE:
                    result.addLine();
                    break;
                case SINGLE_LINE_AFTER_MULTI_LINE_UNDER:
                    if (result.getHeight() > 1) {
                        result.addLine();
                    }
                    break;
                case AFTER_CONDITION:
                default:
                    // Do nothing
                    break;
                }
            }
            result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
        }
        return cacheRenderResult(result, formatContext, parentResult);
    }

}
