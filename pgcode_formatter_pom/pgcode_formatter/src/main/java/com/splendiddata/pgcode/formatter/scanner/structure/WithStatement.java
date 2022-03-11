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
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * The WITH clause of a DML statement. For the formatter the WITH clause is implemented as if it was a statement of its
 * own, containing the actual select, insert update or delete statement.
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class WithStatement extends SrcNode {

    /**
     * The insert, delete, insert or select statement that finishes the CTE(s)
     */
    private ScanResult actualStatement;

    int singleLineWidth;

    /**
     * Constructor
     *
     * @param startNode
     *            The word WITH that starts this statement
     */
    public WithStatement(ScanResult startNode) {
        super(ScanResultType.INTERPRETED, PostgresInputReader.toIdentifier(startNode));
        assert "with".equalsIgnoreCase(startNode.toString()) : "A WithStatement must start with the word WITH, not: "
                + startNode;
        ScanResult priorNode = getStartScanResult().locatePriorToNextInterpretable();
        ScanResult currentNode = priorNode.getNext();
        if (currentNode != null && currentNode.is(ScanResultType.IDENTIFIER)
                && "recursive".equalsIgnoreCase(currentNode.toString())) {
            currentNode = PostgresInputReader.toIdentifier(currentNode);
            priorNode.setNext(currentNode);
            priorNode = currentNode.locatePriorToNextInterpretable();
        }
        currentNode = CommaSeparatedList.withArbitraryEnd(priorNode.getNext(), node -> {
            if (node.is(ScanResultType.IDENTIFIER)) {
                return new WithQuery(node);
            }
            return PostgresInputReader.interpretStatementBody(node);
        }, node -> {
            if (!node.is(ScanResultType.IDENTIFIER)) {
                return false;
            }
            switch (node.toString().toLowerCase()) {
            case "select":
            case "insert":
            case "update":
            case "delete":
                return true;
            default:
                return false;
            }
        });
        if (currentNode != null) {
            priorNode.setNext(currentNode);
            priorNode = currentNode.locatePriorToNextInterpretable();
            currentNode = PostgresInputReader.interpretStatementStart(priorNode.getNext());
            if (currentNode != null) {
                priorNode.setNext(currentNode);
                setNext(currentNode.getNext());
                currentNode.setNext(null);
                actualStatement = currentNode;
            }
        }
    }

    /**
     * @see com.splendiddata.pgcode.formatter.scanner.structure.SrcNode#getText()
     *
     * @return String the text of this with clause
     */
    @Override
    public String getText() {
        return toString();
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        RenderMultiLines renderResult = getCachedRenderResult(formatContext, parentResult, config);
        if (renderResult != null) {
            return renderResult;
        }
        renderResult = new RenderMultiLines(this, formatContext, parentResult);
        if (parentResult != null) {
            renderResult.setIndentBase(parentResult.getPosition());
        }
        if (config.getQueryConfig().isIndent().booleanValue()) {
            renderResult.setIndent(config.getStandardIndent());
        }
        for (ScanResult node = this.getStartScanResult(); node != null; node = node.getNext()) {
            if (node == actualStatement) {
                if (config.getQueryConfig().isIndent().booleanValue()) {
                    renderResult.setIndent(config.getStandardIndent());
                }
                if (renderResult.getHeight() > 1
                        || renderResult.getPosition() + node.getSingleLineWidth(config) > config.getQueryConfig()
                                .getMaxSingleLineQuery().getValue()) {
                    renderResult.addLine();
                }
            }
            renderResult.addRenderResult(node.beautify(formatContext, renderResult, config), formatContext);
        }

        return cacheRenderResult(renderResult, formatContext, parentResult);
    }

    /**
     * @see ScanResult#getSingleLineWidth(FormatConfiguration)
     */
    @Override
    public int getSingleLineWidth(FormatConfiguration config) {
        if (singleLineWidth != 0) {
            return singleLineWidth;
        }
        int currentNodeWidth;
        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            currentNodeWidth = node.getSingleLineWidth(config);
            if (currentNodeWidth < 0) {
                singleLineWidth = -1;
                return singleLineWidth;
            }
            singleLineWidth += currentNodeWidth;
        }
        return singleLineWidth;
    }
}
