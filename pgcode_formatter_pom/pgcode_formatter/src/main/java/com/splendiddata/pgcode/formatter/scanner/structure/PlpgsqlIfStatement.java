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
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.PlpgsqlIfStatementType;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * an IF statement
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class PlpgsqlIfStatement extends SrcNode {
    /**
     * Constructor
     *
     * @param startScanResult
     *            the ScanResult that reads IF
     */
    public PlpgsqlIfStatement(ScanResult startScanResult) {
        super(ScanResultType.INTERPRETED, PostgresInputReader.toIdentifier(startScanResult));
        assert "if".equalsIgnoreCase(startScanResult
                .toString()) : "An if statement must start with the word IF, not with: " + startScanResult;
        ScanResult lastInterpreted = getStartScanResult();
        ScanResult priorNode = lastInterpreted;
        ScanResult currentNode;
        for (currentNode = priorNode.getNext(); currentNode != null
                && !currentNode.isStatementEnd(); currentNode = priorNode.getNext()) {
            currentNode = PostgresInputReader.interpretStatementBody(currentNode);
            priorNode.setNext(currentNode);
            if (currentNode.getType().isInterpretable()) {
                lastInterpreted = currentNode;
            }
            if (currentNode.is(ScanResultType.IDENTIFIER)) {
                switch (currentNode.toString().toLowerCase()) {
                case "then":
                case "else":
                    priorNode = currentNode;
                    /*
                     * skip whitespace and comment
                     */
                    for (currentNode = priorNode.getNext(); currentNode != null && !currentNode.isStatementEnd()
                            && !currentNode.getType().isInterpretable(); currentNode = priorNode.getNext()) {
                        currentNode = PostgresInputReader.interpretStatementBody(currentNode);
                        priorNode.setNext(currentNode);
                        priorNode = currentNode;
                    }
                    /*
                     * code between then, elsif, else and end
                     */
                    if (currentNode != null && !currentNode.isStatementEnd()) {
                        currentNode = new PlpgsqlCompoundStatementPart(currentNode, node -> {
                            if (node.is(ScanResultType.IDENTIFIER)) {
                                switch (node.toString().toLowerCase()) {
                                case "else":
                                case "elsif":
                                case "end":
                                    return true;
                                default:
                                    break;
                                }
                            }
                            return false;
                        });
                        priorNode.setNext(currentNode);
                        lastInterpreted = currentNode;
                    }
                    break;
                default:
                    break;
                }
            }
            priorNode = currentNode;
        }
        if (currentNode != null && currentNode.is(ScanResultType.SEMI_COLON)) {
            lastInterpreted = new SemiColonNode(currentNode);
            priorNode.setNext(lastInterpreted);
        }
        setNext(lastInterpreted.getNext());
        lastInterpreted.setNext(null);
    }
    

    /**
     * @see SrcNode#getBeginEndLevel()
     *
     * @return int the beginEndLevel after END IF;
     */
    @Override
    public int getBeginEndLevel() {
        /*
         * IF increments the nesting level, but END IF decrements it. getBeginEndLevel() should return
         * the level AFTER the node, so should return that of the start node minus 1.
         */
        return super.getBeginEndLevel() - 1;
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
        int conditionIndent;
        int parentPosition = parentResult == null ? 0 : parentResult.getPosition();

        PlpgsqlIfStatementType ifConfig = config.getLanguagePlpgsql().getCodeSection().getIfStatement();
        renderResult = new RenderMultiLines(this, formatContext, parentResult).setIndentBase(parentPosition).setIndent(config.getStandardIndent());
        ScanResult node = getStartScanResult();
        renderResult.addRenderResult(node.beautify(formatContext, renderResult, config), formatContext);
        renderResult.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
        boolean conditionStart = true;
        for (node = node.getNext(); node != null; node = (node == null ? null : node.getNext())) {
            if (node.is(ScanResultType.IDENTIFIER)) {
                switch (node.toString().toLowerCase()) {
                case "elsif":
                    renderResult.positionAt(parentPosition);
                    renderResult.addRenderResult(node.beautify(formatContext, renderResult, config), formatContext);
                    conditionStart = true;
                    continue;
                case "else":
                    renderResult.positionAt(parentPosition);
                    renderResult.addRenderResult(node.beautify(formatContext, renderResult, config), formatContext);
                    renderResult.addLine();
                    continue;
                case "end":
                    renderResult.positionAt(parentPosition);
                    renderResult.addRenderResult(node.beautify(formatContext, renderResult, config), formatContext);
                    continue;
                default:
                    break;
                }
            }
            if (conditionStart) {
                conditionStart = false;
                switch (ifConfig.getConditionIndent()) {
                case DOUBLE_INDENTED:
                    conditionIndent = 2 * config.getStandardIndent();
                    break;
                case UNDER_FIRST_ARGUMENT:
                    conditionIndent = node.toString().length() + 1;
                    break;
                case INDENTED:
                default:
                    conditionIndent = config.getStandardIndent();
                    break;
                }
                renderResult.setIndent(conditionIndent);
                int currentHeight = renderResult.getHeight();
                FormatContext contentContext = new FormatContext(config, formatContext)
                        .setAvailableWidth(formatContext.getAvailableWidth() - conditionIndent);
                for (; node != null && !(node.is(ScanResultType.IDENTIFIER)
                        && "then".equalsIgnoreCase(node.toString())); node = node.getNext()) {
                    RenderResult nodeResult = node.beautify(contentContext, renderResult, config);
                    int pos = renderResult.getPosition();
                    if (pos > config.getStandardIndent() && pos + nodeResult.getWidth()
                            + config.getStandardIndent() > formatContext.getAvailableWidth()) {
                        renderResult.addLine();
                    }
                    renderResult.addRenderResult(nodeResult, contentContext);
                }
                renderResult.setIndent(config.getStandardIndent());
                switch (ifConfig.getThen()) {
                case AFTER_CONDITION:
                    break;
                case ON_NEW_LINE:
                    renderResult.positionAt(parentPosition);
                    break;
                case SINGLE_LINE_AFTER_MULTI_LINE_UNDER:
                    if (renderResult.getHeight() - currentHeight > 1) {
                        renderResult.positionAt(parentPosition);
                    }
                    break;
                default:
                    assert false : new StringBuilder().append("Unknown ")
                            .append(ifConfig.getThen().getClass().getName()).append(":").append(ifConfig.getThen())
                            .append(" in ").append(getClass().getName());
                }
                renderResult.addRenderResult(node.beautify(formatContext, renderResult, config), formatContext);
                renderResult.addLine();
                continue;
            }
            if (node != null) {
                renderResult.addRenderResult(node.beautify(formatContext, renderResult, config), formatContext);
            }
        }
        return cacheRenderResult(renderResult, formatContext, parentResult);
    }

    /**
     * @see ScanResult#getSingleLineWidth(FormatConfiguration)
     *
     * @return -1 as an IF statement is a compound statement, so will never fit on a single line
     */
    @Override
    public int getSingleLineWidth(FormatConfiguration config) {
        return -1;
    }

}
