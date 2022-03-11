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

import java.util.function.Function;
import java.util.regex.Pattern;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.CaseFormatContext;
import com.splendiddata.pgcode.formatter.internal.CaseFormatContext.RenderPhase;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.internal.Util;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Can be a when clause of a case clause or a when part in a case statement or a when statement in exception handling
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class WhenClauseNode extends SrcNode {
    private static final Pattern END_OF_WHEN_PATTERN = Pattern.compile("^(WHEN|ELSE|END)$", Pattern.CASE_INSENSITIVE);
    private final boolean isStatement;
    private SrcNode whenExpression;
    private ScanResult thenExpression;
    private RenderMultiLines partialResult;
    int singleLineLength;

    /**
     * Constructor
     *
     * @param startNode
     *            The node that starts this when statement or clause. This may be whitespace or comment, but the first
     *            effective node must be 'WHEN'.
     * @param interpreter
     *            The interpreter to use for the content of the when clause.
     * @param isStatement
     *            Is this when clause part of a case statement (true) or a case clause (false)
     */
    public WhenClauseNode(ScanResult startNode, Function<ScanResult, SrcNode> interpreter, boolean isStatement) {
        super(ScanResultType.WHEN_THEN_CLAUSE, startNode);
        this.isStatement = isStatement;
        ScanResult cur = startNode;
        if (!"when".equalsIgnoreCase(cur.toString())) {
            // a when statement may start with comment
            cur = cur.getNextInterpretable();
        }
        assert "when".equalsIgnoreCase(cur.toString()) : "Expecting 'WHEN' but got: " + cur;
        whenExpression = PostgresInputReader.interpretStatementBody(cur.getNext());
        cur.setNext(null);
        ScanResult prior = whenExpression;
        for (cur = whenExpression.getNext(); !cur.isEof()
                && !"then".equalsIgnoreCase(cur.toString()); cur = prior.getNext()) {
            cur = PostgresInputReader.interpretStatementBody(cur);
            prior.setNext(cur);
            prior = cur;
        }
        prior.setNext(null);
        if (cur.isEof()) {
            setNext(cur);
            return;
        }
        thenExpression = cur;
        for (cur = cur.getNextInterpretable(); !cur.isEof()
                && !END_OF_WHEN_PATTERN.matcher(cur.toString()).matches(); cur = cur.getNextInterpretable()) {
            if (cur.isStatementEnd()) {
                break;
            }
        }
        prior = thenExpression;
        for (cur = thenExpression.getNext(); cur != null && !cur.isEof()
                && !END_OF_WHEN_PATTERN.matcher(cur.toString()).matches(); cur = prior.getNext()) {
            if (cur.getType().isInterpretable()) {
                cur = interpreter.apply(cur);
            }
            prior.setNext(cur);
            prior = cur;
        }
        prior.setNext(null);
        setNext(cur);
    }

    /**
     * @see java.lang.Object#toString()
     *
     * @return String for debugging purposes
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            str.append(node);
        }
        for (ScanResult node = whenExpression; node != null; node = node.getNext()) {
            str.append(node);
        }
        for (ScanResult node = thenExpression; node != null; node = node.getNext()) {
            str.append(node);
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
        CaseFormatContext context = formatContext instanceof CaseFormatContext ? (CaseFormatContext) formatContext
                : new CaseFormatContext(config, formatContext, config.getCaseWhen());
        
        int parentPosition = 0;
        if (parentResult != null) {
            if (isStatement && !parentResult.isLastNonWhiteSpaceEqualToLinefeed()) {
                parentResult.addLine();
            }
            parentPosition = parentResult.getPosition();
        }

        /*
         * First see if we are rendering linear
         */
        if (RenderPhase.RENDER_LINEAR.equals(context.getRenderPhase())) {
            result = new RenderMultiLines(this, context, parentResult).setIndentBase(parentPosition);
            for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
                result.addRenderResult(node.beautify(context, result, config), context);
            }
            for (ScanResult node = whenExpression; node != null; node = node.getNext()) {
                result.addRenderResult(node.beautify(context, result, config), context);
            }
            for (ScanResult node = thenExpression; node != null; node = node.getNext()) {
                result.addRenderResult(node.beautify(context, result, config), context);
            }
            return cacheRenderResult(result, formatContext, parentResult);
        }

        if (RenderPhase.DETERMINE_THEN_POSITION.equals(context.getRenderPhase())) {
            partialResult = new RenderMultiLines(this, context, parentResult).setIndentBase(parentPosition);
            for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
                partialResult.addRenderResult(node.beautify(context, partialResult, config), context);
            }
            for (ScanResult node = whenExpression; node != null; node = node.getNext()) {
                partialResult.addRenderResult(node.beautify(context, partialResult, config), context);
            }
            partialResult.removeTrailingSpaces();
            context.maximizeThenPosition(partialResult.getPosition() + 1);
            return null;
        }

        result = partialResult;
        if (result == null) {
            result = new RenderMultiLines(this, context, parentResult).setIndentBase(parentPosition);
            for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
                result.addRenderResult(node.beautify(context, result, config), context);
            }
            for (ScanResult node = whenExpression; node != null; node = node.getNext()) {
                result.addRenderResult(node.beautify(context, result, config), context);
            }
        } else {
            partialResult = null;
        }
        switch (context.getCaseConfig().getThenPosition().getValue()) {
        case THEN_AFTER_WHEN_ALIGNED:
            result.positionAt(context.getThenPosition());
            break;
        case THEN_AFTER_WHEN_DIRECTLY:
            break;
        case THEN_INDENTED:
        case THEN_UNDER_WHEN:
            result.addLine(Util.nSpaces(context.getThenPosition()));
            result.setIndentBase(context.getThenPosition());
        default:
            break;

        }
        if (isStatement) {
            result.setIndent(config.getStandardIndent());
        } else {
            result.setIndent("THEN ".length());
        }
        ScanResult node = thenExpression;
        result.addRenderResult(node.beautify(context, result, config), context);
        for (node = node.getNext(); node != null; node = node.getNext()) {
            if (isStatement && node.getType().isInterpretable()) {
                result.addLine();
            }
            result.addRenderResult(node.beautify(context, result, config), context);
        }

        result.removeTrailingSpaces();
        return cacheRenderResult(result, formatContext, parentResult);
    }

    /**
     * @see ScanResult#getSingleLineWidth(FormatConfiguration)
     */
    @Override
    public int getSingleLineWidth(FormatConfiguration config) {
        if (singleLineLength != 0) {
            return singleLineLength;
        }
        int elementSize;
        for (ScanResult node = whenExpression; node != null; node = node.getNext()) {
            elementSize = node.getSingleLineWidth(config);
            if (elementSize < 0) {
                singleLineLength = 0;
                return singleLineLength;
            }
            singleLineLength += elementSize;
        }
        for (ScanResult node = thenExpression; node != null; node = node.getNext()) {
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
