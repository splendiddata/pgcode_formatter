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
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CaseType;
import com.splendiddata.pgcode.formatter.internal.CaseFormatContext;
import com.splendiddata.pgcode.formatter.internal.CaseFormatContext.RenderPhase;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.internal.Util;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A case statement - not to be mistaken for a case clause, which is part of a statement.
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class CaseClauseNode extends SrcNode {
    private static final Logger log = LogManager.getLogger(CaseClauseNode.class);

    private SrcNode caseExpression;
    private List<WhenClauseNode> whenClauses = new ArrayList<>();
    private SrcNode elseExpression;
    private ScanResult endNode;

    private int singleLineLength;

    /**
     * Constructor
     *
     * @param startNode
     *            The node that contains "case"
     */
    public CaseClauseNode(ScanResult startNode) {
        super(ScanResultType.CASE_CLAUSE, new IdentifierNode(startNode));
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
            if (priorNode != null) {
                for (nextNode = priorNode.getNext(); nextNode != null && !nextNode.isEof()
                        && !"when".equalsIgnoreCase(nextNode.getText()); nextNode = priorNode.getNext()) {
                    nextNode = PostgresInputReader.interpretStatementBody(nextNode);
                    priorNode.setNext(nextNode);
                    priorNode = nextNode;
                }
                priorNode.setNext(null);
            }
        }
        while (nextNode != null && "when".equalsIgnoreCase(nextNode.getText())) {
            nextNode = new WhenClauseNode(nextNode,
                    (ScanResult node) -> PostgresInputReader.interpretStatementBody(node), false);
            whenClauses.add((WhenClauseNode) nextNode);
            priorNode = nextNode;
            nextNode = nextNode.getNext();
            priorNode.setNext(null);
        }
        if (nextNode != null && "else".equalsIgnoreCase(nextNode.getText())) {
            elseExpression = PostgresInputReader.interpretStatementBody(nextNode);
            priorNode = elseExpression;
            for (nextNode = priorNode.getNext(); nextNode != null && !nextNode.isEof()
                    && !"end".equalsIgnoreCase(nextNode.getText()); nextNode = priorNode.getNext()) {
                nextNode = PostgresInputReader.interpretStatementBody(nextNode);
                priorNode.setNext(nextNode);
                priorNode = nextNode;
            }
            priorNode.setNext(null);
        }
        if (nextNode != null && "end".equalsIgnoreCase(nextNode.getText())) {
            endNode = new IdentifierNode(nextNode);
            nextNode = nextNode.getNext();
            endNode.setNext(null);
        }
        setNext(nextNode);
    }

    /**
     * @see java.lang.Object#toString()
     *
     * @return String a debug representation of this case clause
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            str.append(node);
        }
        for (ScanResult node = caseExpression; node != null; node = node.getNext()) {
            str.append(node);
        }
        for (WhenClauseNode node : whenClauses) {
            str.append(node);
        }
        if (elseExpression != null) {
            for (ScanResult node = elseExpression; node != null; node = node.getNext()) {
                str.append(node);
            }
        }
        if (endNode != null) {
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
        RenderMultiLines result = getCachedRenderResult(formatContext, parentResult, config);
        if (result != null) {
            return result;
        }

        CaseFormatContext context = new CaseFormatContext(config, formatContext,
                caseExpression == null ? config.getCaseWhen() : config.getCaseOperand());
        context.setRenderPhase(RenderPhase.RENDER_LINEAR);
        int parentPosition = 0;
        if (parentResult != null) {
            parentPosition = parentResult.getPosition();
        }

        /*
         * First see if a single line rendering will fit
         */
        int singleLineWidth = getSingleLineWidth(config);
        if (singleLineWidth > 0 && singleLineWidth + parentPosition <= config.getLineWidth().getValue()) {
            result = new RenderMultiLines(this, context, parentResult);
            for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
                result.addRenderResult(node.beautify(context, result, config), context);
            }
            if (caseExpression != null) {
                for (ScanResult node = caseExpression; node != null; node = node.getNext()) {
                    result.addRenderResult(node.beautify(context, result, config), context);
                }
            }
            for (WhenClauseNode whenClause : whenClauses) {
                for (ScanResult node = whenClause; node != null; node = node.getNext()) {
                    result.addRenderResult(node.beautify(context, result, config), context);
                    result.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
                }
            }
            result.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
            for (ScanResult node = elseExpression; node != null; node = node.getNext()) {
                result.addRenderResult(node.beautify(context, result, config), context);
            }
            result.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
            for (ScanResult node = endNode; node != null; node = node.getNext()) {
                result.addRenderResult(node.beautify(context, result, config), context);
            }
            if (result.getHeight() <= 1) {
                return cacheRenderResult(result, formatContext, parentResult);
            }
        }

        /*
         * Single line rendering didn't work. So try multi-line now
         */
        result = new RenderMultiLines(this, context, parentResult);
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
            result.setIndent(elsePosition + "ELSE ".length());
            result.positionAt(elsePosition);
            for (ScanResult node = elseExpression; node != null; node = node.getNext()) {
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
        }

        result.removeTrailingSpaces();
        if (log.isTraceEnabled()) {
            RenderMultiLines copy = result.clone();
            log.trace("beautify with " + Util.xmlBeanToString(context.getCaseConfig()) + " =\n" + copy.beautify());
        }
        return cacheRenderResult(result, formatContext, parentResult);
    }

    /**
     * @see ScanResult#getSingleLineWidth(FormatConfiguration)
     */
    @Override
    public int getSingleLineWidth(FormatConfiguration config) {
        if (singleLineLength != 0) {
            /*
             * Been here before, so the answer can be given rapidly
             */
            return singleLineLength;
        }
        CaseType caseConfig = caseExpression == null ? config.getCaseWhen() : config.getCaseOperand();
        if (caseConfig.getMaxSingleLineClause().getWeight() < caseConfig.getWhenPosition().getWeight()) {
            switch (caseConfig.getWhenPosition().getValue()) {
            case WHEN_INDENTED:
            case WHEN_UNDER_CASE:
                singleLineLength = -1;
                return singleLineLength;
            case WHEN_AFTER_CASE:
            default:
                break;
            }
        }
        if (caseConfig.getMaxSingleLineClause().getWeight() < caseConfig.getThenPosition().getWeight()) {
            switch (caseConfig.getThenPosition().getValue()) {
            case THEN_INDENTED:
            case THEN_UNDER_WHEN:
                singleLineLength = -1;
                return singleLineLength;
            case THEN_AFTER_WHEN_ALIGNED:
            case THEN_AFTER_WHEN_DIRECTLY:
            default:
                break;
            }
        }

        int elementSize;
        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            elementSize = node.getSingleLineWidth(config);
            if (elementSize < 0) {
                singleLineLength = 0;
                return singleLineLength;
            }
            singleLineLength += elementSize;
        }
        if (caseExpression != null) {
            for (ScanResult node = caseExpression; node != null; node = node.getNext()) {
                elementSize = node.getSingleLineWidth(config);
                if (elementSize < 0) {
                    singleLineLength = 0;
                    return singleLineLength;
                }
                singleLineLength += elementSize;
            }
        }
        for (WhenClauseNode whenClause : whenClauses) {
            for (ScanResult node = whenClause; node != null; node = node.getNext()) {
                elementSize = node.getSingleLineWidth(config);
                if (elementSize < 0) {
                    singleLineLength = 0;
                    return singleLineLength;
                }
                singleLineLength += elementSize;
            }
        }
        for (ScanResult node = elseExpression; node != null; node = node.getNext()) {
            elementSize = node.getSingleLineWidth(config);
            if (elementSize < 0) {
                singleLineLength = 0;
                return singleLineLength;
            }
            singleLineLength += elementSize;
        }
        for (ScanResult node = endNode; node != null; node = node.getNext()) {
            elementSize = node.getSingleLineWidth(config);
            if (elementSize < 0) {
                singleLineLength = 0;
                return singleLineLength;
            }
            singleLineLength += elementSize;
        }
        return singleLineLength;
    }

}
