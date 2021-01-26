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
        assert "if".equalsIgnoreCase(
                startScanResult.toString()) : "An if statement must start with the word IF, not with: "
                        + startScanResult;

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
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult, FormatConfiguration config) {
        int standardIndent = FormatContext.indent(true).length();
        int conditionIndent;
        PlpgsqlIfStatementType ifConfig = FormatConfiguration.getEffectiveConfiguration().getLanguagePlpgsql()
                .getCodeSection().getIfStatement();
        RenderMultiLines result = new RenderMultiLines(this, formatContext).setIndent(0);
        ScanResult node = getStartScanResult();
        result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
        result.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
        boolean conditionStart = true;
        for (node = node.getNext(); node != null; node = (node == null ? null : node.getNext())) {
            if (node.is(ScanResultType.IDENTIFIER)) {
                switch (node.toString().toLowerCase()) {
                case "elsif":
                    conditionStart = true;
                    //$FALL-THROUGH$
                case "else":
                    result.removeTrailingLineFeeds();
                    result.positionAt(0);
                    break;
                default:
                    break;
                }
            }
            if (conditionStart) {
                conditionStart = false;
                switch (ifConfig.getConditionIndent()) {
                case DOUBLE_INDENTED:
                    conditionIndent = 2 * standardIndent;
                    break;
                case UNDER_FIRST_ARGUMENT:
                    conditionIndent = node.toString().length() + 1;
                    break;
                case INDENTED:
                default:
                    conditionIndent = standardIndent;
                    break;
                }
                result.setIndent(conditionIndent);
                int currentHeight = result.getHeight();
                FormatContext contentContext = new FormatContext(config, formatContext)
                        .setAvailableWidth(formatContext.getAvailableWidth() - conditionIndent);
                for (; node != null && !(node.is(ScanResultType.IDENTIFIER)
                        && "then".equalsIgnoreCase(node.toString())); node = node.getNext()) {
                    RenderResult nodeResult = node.beautify(contentContext, result, config);
                    int pos = result.getPosition();
                    if (pos > standardIndent
                            && pos + nodeResult.getWidth() + standardIndent > formatContext.getAvailableWidth()) {
                        result.addLine();
                    }
                    result.addRenderResult(nodeResult, contentContext);
                }
                result.setIndent(0);
                switch (ifConfig.getThen()) {
                case AFTER_CONDITION:
                    break;
                case ON_NEW_LINE:
                    result.positionAt(0);
                    break;
                case SINGLE_LINE_AFTER_MULTI_LINE_UNDER:
                    if (result.getHeight() - currentHeight > 1) {
                        result.positionAt(0);
                    }
                    break;
                default:
                    assert false : "Unknown com.splendiddata.pgcode.formatter.configuration.xml.v1_0.PlpgsqlConditionEndPositionType: "
                            + ifConfig.getThen();
                }
            }
            if (node != null) {
                if (node instanceof PlpgsqlCompoundStatementPart) {
                    result.setIndent(FormatContext.indent(true));
                    result.positionAt(formatContext.getIndent() + standardIndent);
                    RenderResult nodeResult = node.beautify(formatContext, result, config);
                    result.addRenderResult(nodeResult, formatContext);
                    result.setIndent(0);
                    result.addLine();
                } else {
                    result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
                }
            }
        }
        return result;
    }
}
