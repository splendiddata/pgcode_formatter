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
import com.splendiddata.pgcode.formatter.internal.Util;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A WHILE loop.
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class WhileLoopNode extends SrcNode {

    /**
     * Constructor
     *
     * @param startNode
     *            The node that contains WHILE
     */
    public WhileLoopNode(ScanResult startNode) {
        super(ScanResultType.INTERPRETED, PostgresInputReader.toIdentifier(startNode));
        assert "while".equalsIgnoreCase(startNode.getText()) : "Expecting WHILE but found: " + startNode.getText();
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
        RenderMultiLines renderResult = getCachedRenderResult(formatContext, parentResult, config);
        if (renderResult != null) {
            return renderResult;
        }
        int parentPosition = 0;
        if (parentResult != null) {
            parentPosition = parentResult.getPosition();
        }
        renderResult = new RenderMultiLines(this, formatContext, parentResult).setIndentBase(parentPosition);
        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            if (node instanceof LoopNode) {
                switch (config.getLanguagePlpgsql().getCodeSection().getForStatement().getLoop()) {
                case ON_NEW_LINE:
                    renderResult.addLine(Util.nSpaces(config.getStandardIndent()));
                    break;
                case SINGLE_LINE_AFTER_MULTI_LINE_UNDER:
                    if (renderResult.getHeight() > 1) {
                        renderResult.addLine(Util.nSpaces(config.getStandardIndent()));
                    }
                    break;
                case AFTER_CONDITION:
                default:
                    break;
                }
            }
            renderResult.addRenderResult(node.beautify(formatContext, renderResult, config), formatContext);
        }
        return cacheRenderResult(renderResult, formatContext, parentResult);
    }

    /**
     * @see ScanResult#getSingleLineWidth(FormatConfiguration)
     *
     * @return -1 as a loop is a compound statement
     */
    @Override
    public int getSingleLineWidth(FormatConfiguration config) {
        return -1;
    }

}
