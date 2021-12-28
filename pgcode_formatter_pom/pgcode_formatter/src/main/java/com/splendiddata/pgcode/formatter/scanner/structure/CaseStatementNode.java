/*
 * Copyright (c) Splendid Data Product Development B.V. 2020 - 2021
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

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.*;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A case clause - not to be mistaken for a case statement.
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class CaseStatementNode extends SrcNode {
    private static final Logger log = LogManager.getLogger(CaseStatementNode.class);
    private SrcNode caseExpression;
    private List<WhenClauseNode> whenClauses = new ArrayList<>();
    private SrcNode elseExpression;
    private ScanResult endNode;
    private boolean caseWithOperand = false;

    /**
     * Constructor
     *
     * @param startNode
     *            The node that contains "case"
     */
    public CaseStatementNode(ScanResult startNode) {
        super(ScanResultType.CASE_STATEMENT, startNode);
        assert "case".equalsIgnoreCase(startNode.getText()) : "Expecting 'CASE' but found: " + startNode.getText();
        ScanResult priorNode = startNode;
        ScanResult nextNode;
        for (nextNode = startNode.getNext(); !nextNode.isEof()
                && !nextNode.getType().isInterpretable(); nextNode = priorNode.getNext()) {
            priorNode = nextNode;
            caseWithOperand = true;
        }
        priorNode.setNext(null);
        if ("when".equalsIgnoreCase(nextNode.getText())) {
            caseExpression = null;
        } else {
            caseExpression = PostgresInputReader.interpretStatementBody(nextNode);
            priorNode = caseExpression;
            for (nextNode = priorNode.getNext(); !nextNode.isEof()
                    && !"when".equalsIgnoreCase(nextNode.getText()); nextNode = priorNode.getNext()) {
                nextNode = PostgresInputReader.interpretStatementBody(nextNode);
                priorNode.setNext(nextNode);
                priorNode = nextNode;
            }
            priorNode.setNext(null);
        }
        while ("when".equalsIgnoreCase(nextNode.getText())) {
            nextNode = new WhenClauseNode(nextNode,
                    (ScanResult node) -> PostgresInputReader.interpretStatementStart(node));
            whenClauses.add((WhenClauseNode) nextNode);
            priorNode = nextNode;
            nextNode = nextNode.getNext();
            priorNode.setNext(null);
        }
        if ("else".equalsIgnoreCase(nextNode.getText())) {
            elseExpression = PostgresInputReader.interpretStatementBody(nextNode);
            priorNode = elseExpression;
            for (nextNode = priorNode.getNext(); !nextNode.isEof()
                    && !"end".equalsIgnoreCase(nextNode.getText()); nextNode = priorNode.getNext()) {
                nextNode = PostgresInputReader.interpretStatementBody(nextNode);
                priorNode.setNext(nextNode);
                priorNode = nextNode;
            }
            priorNode.setNext(null);
        }
        if ("end".equalsIgnoreCase(nextNode.getText())) {
            endNode = nextNode;
            priorNode = nextNode;
            nextNode = nextNode.getNext();

            for (nextNode = nextNode.getNext(); !nextNode.isEof()
                    && !nextNode.is(ScanResultType.SEMI_COLON); nextNode = nextNode.getNext()) {
                priorNode = nextNode;
            }
        }

        setNext(nextNode);
        priorNode.setNext(null);
        log.debug(() -> toString());
    }

    /**
     * @see java.lang.Object#toString()
     *
     * @return String a debug representation of this case clause
     */
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            str.append(node);
        }
        str.append(" ");
        for (ScanResult node = caseExpression; node != null; node = node.getNext()) {
            str.append(node);
        }
        for (WhenClauseNode node : whenClauses) {
            str.append(node);
        }
        if (elseExpression != null) {
            str.append(elseExpression);
        }
        if (endNode != null) {
            str.append(" ");
            str.append(endNode);
        }
        return str.toString();
    }

    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        FormatContext context = new FormatContext(config, formatContext);
        if (caseWithOperand) {
            context.setCaseType(config.getCaseOperand());
        } else {
            context.setCaseType(config.getCaseWhen());
        }
        RenderMultiLines result = new RenderMultiLines(this, context);

        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            ScanResult current = Util.interpretStatement(node);

            RenderResult renderResult = current.beautify(context, result, config);
            result.addRenderResult(renderResult, context);

        }

        for (ScanResult node = caseExpression; node != null; node = node.getNext()) {
            ScanResult current = Util.interpretStatement(node);

            RenderResult renderResult = current.beautify(context, result, config);
            result.addRenderResult(renderResult, context);
        }

        boolean newLineExists = result.isLastNonWhiteSpaceEqualToLinefeed();
        for (WhenClauseNode node : whenClauses) {
            RenderMultiLines whenClauseResult = (RenderMultiLines) node.beautify(context, result, config);

            switch (context.getCaseType().getWhenPosition().getValue()) {
            case WHEN_UNDER_CASE:
                whenClauseResult.addLineAtStart(newLineExists);
                break;
            case WHEN_INDENTED:
                result.removeTrailingSpaces();
                result.addLine(FormatContext.indent(true));
                break;
            case WHEN_AFTER_CASE:
                result.addWhiteSpaceIfApplicable();
                break;
            default:
                break;
            }

            result.addRenderResult(whenClauseResult, context);
            newLineExists = result.isLastNonWhiteSpaceEqualToLinefeed();
        }

        if (elseExpression != null) {

            for (ScanResult node = elseExpression; node != null; node = node.getNext()) {
                ScanResult current = Util.interpretStatement(node);

                RenderResult renderResult = current.beautify(context, result, config);
                newLineExists = result.isLastNonWhiteSpaceEqualToLinefeed();
                switch (context.getCaseType().getElsePosition()) {
                case ELSE_UNDER_WHEN:
                case ELSE_UNDER_THEN:
                    if (current instanceof IdentifierNode && "else".equalsIgnoreCase(current.toString())) {
                        if (!newLineExists) {
                            result.addLine("");
                        }
                    }
                    break;
                default:
                    // do nothing
                    break;
                }
                result.addRenderResult(renderResult, context);
            }
        }

        result.removeTrailingSpaces();

        if (endNode != null) {

            for (ScanResult node = endNode; node != null; node = node.getNext()) {
                ScanResult current = Util.interpretStatement(node);
                RenderResult renderResult = current.beautify(context, result, config);
                switch (context.getCaseType().getEndPosition()) {
                case END_UNDER_CASE:
                    result.addLine("");
                    break;
                case END_UNDER_WHEN:
                    result.addLine("");
                    break;
                case END_AT_SAME_LINE:
                default:
                    // do nothing
                    break;
                }

                result.addRenderResult(renderResult, context);
            }
        }

        return result;
    }
}
