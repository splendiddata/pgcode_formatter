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

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.*;
import com.splendiddata.pgcode.formatter.internal.CaseFormatContext.RenderPhase;
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
                    (ScanResult node) -> PostgresInputReader.interpretStatementStart(node), true);
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
                nextNode = PostgresInputReader.interpretStatementStart(nextNode);
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
     * @see SrcNode#getBeginEndLevel()
     *
     * @return int the beginEndLevel at the end of the case statement
     */
    @Override
    public int getBeginEndLevel() {
        /*
         * CASE increments the nesting level, but END CASE decrements it. getBeginEndLevel() should return
         * the level AFTER the node, so should return that of the start node minus 1.
         */
        return super.getBeginEndLevel() - 1;
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

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        CaseFormatContext context = new CaseFormatContext(config, formatContext,
                caseExpression == null ? config.getCaseWhen() : config.getCaseOperand());
        RenderMultiLines result = new RenderMultiLines(this, context, parentResult);
        context.setRenderPhase(RenderPhase.RENDER_NORMAL);
        int casePosition = 0;
        if (parentResult != null) {
            casePosition = parentResult.getPosition();
        }
        result.setIndent(casePosition + "CASE ".length());

        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            result.addRenderResult(node.beautify(context, result, config), context);
        }

        for (ScanResult node = caseExpression; node != null; node = node.getNext()) {
            result.addRenderResult(node.beautify(context, result, config), context);
        }
        result.removeTrailingSpaces();
        result.setIndent(0);

        /*
         * The WHEN result will be rendered into the parentResult, so do take the parentResult's indenting into account.
         */
        int whenPosition;
        switch (context.getCaseConfig().getWhenPosition().getValue()) {
        case WHEN_AFTER_CASE:
            whenPosition = result.getPosition();
            break;
        case WHEN_INDENTED:
            whenPosition = casePosition + config.getStandardIndent();
            result.addLine(whenPosition > 0 ? Util.nSpaces(whenPosition) : "");
            break;
        case WHEN_UNDER_CASE:
        default:
            whenPosition = casePosition;
            result.addLine(Util.nSpaces(whenPosition));
            break;
        }
        /*
         * THEN will be rendered into the WHEN result, so positioning is relative to WHEN
         */
        switch (context.getCaseConfig().getThenPosition().getValue()) {
        case THEN_AFTER_WHEN_ALIGNED:
            context.setThenPosition(context.getCaseConfig().getThenPosition().getMinPosition().intValue());
            context.setRenderPhase(RenderPhase.DETERMINE_THEN_POSITION);
            break;
        case THEN_AFTER_WHEN_DIRECTLY:
        case THEN_INDENTED:
            context.setThenPosition(whenPosition + config.getStandardIndent());
            break;
        case THEN_UNDER_WHEN:
            context.setThenPosition(whenPosition);
            break;
        default:
            break;
        }

        if (RenderPhase.DETERMINE_THEN_POSITION.equals(context.getRenderPhase())) {
            /*
             * To align the position of THEN properly, we have to partially render every when clause (the THEN position
             * will be stored in the render context).
             */
            for (WhenClauseNode node : whenClauses) {
                node.beautify(context, result, config);
            }
        }

        context.setRenderPhase(RenderPhase.RENDER_NORMAL);
        for (WhenClauseNode node : whenClauses) {
            result.positionAt(whenPosition);
            result.addRenderResult(node.beautify(context, result, config), context);
        }
        result.removeTrailingSpaces();

        if (elseExpression != null) {
            int elsePosition;
            switch (context.getCaseConfig().getElsePosition()) {
            case ELSE_UNDER_WHEN:
                elsePosition = whenPosition;
                break;
            case ELSE_UNDER_THEN:
                elsePosition = context.getThenPosition();
                break;
            default:
                elsePosition = casePosition;
                break;
            }
            result.positionAt(elsePosition);
            result.setIndentBase(result.getPosition());
            result.setIndent(config.getStandardIndent());
            ScanResult node = elseExpression;
            result.addRenderResult(node.beautify(context, result, config), context);
            for (node = node.getNext(); node != null; node = node.getNext()) {
                if (node.getType().isInterpretable()) {
                    result.addLine();
                }
                result.addRenderResult(node.beautify(context, result, config), context);
            }
        }

        if (endNode != null) {
            switch (context.getCaseConfig().getEndPosition()) {
            case END_UNDER_CASE:
                result.positionAt(casePosition);
                break;
            case END_UNDER_WHEN:
                result.positionAt(whenPosition);
                break;
            case END_AT_SAME_LINE:
            default:
                // do nothing
                break;
            }
            result.addRenderResult(endNode.beautify(context, result, config), context);
            for (ScanResult node = endNode.getNext(); node != null; node = node.getNext()) {
                result.addRenderResult(node.beautify(context, result, config), context);
            }
        }

        result.removeTrailingSpaces();
        if (log.isTraceEnabled()) {
            RenderMultiLines copy = result.clone();
            log.trace("beautify with " + Util.xmlBeanToString(context.getCaseConfig()) + " =\n" + copy.beautify());
        }
        return result;
    }

    /**
     * @see ScanResult#getSingleLineWidth(FormatConfiguration)
     *
     * @return -1 as a case statement will never fit on a single line
     */
    @Override
    public int getSingleLineWidth(FormatConfiguration config) {
        return -1;
    }

}
