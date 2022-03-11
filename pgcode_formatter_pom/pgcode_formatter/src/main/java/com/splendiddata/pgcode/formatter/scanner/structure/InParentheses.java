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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.Util;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A set of parentheses with content
 * 
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class InParentheses extends SrcNode {
    private static final Logger log = LogManager.getLogger(InParentheses.class);

    private int singleLineWidth;

    /**
     * Constructor that assumes that the content is a comma separated list of which the content is to be interpreted by
     * {@link PostgresInputReader#interpretPlpgsqlStatementStart(ScanResult)}.
     * 
     * @param start
     *            The opening parenthesis
     */
    public InParentheses(ScanResult start) {
        this(start,
                contentNode -> CommaSeparatedList
                        .withArbitraryEnd(contentNode,
                                innerNode -> PostgresInputReader.interpretStatementBody(innerNode), innerNode -> false)
                        .setParentIsParentheses());
    }

    /**
     * Constructor
     *
     * @param start
     *            The opening parenthesis
     * @param contentInterpreter
     *            Function&lt;ScanResult, ScanResult&gt; The interpreter that is responsible for interpreting the
     *            content
     */
    public InParentheses(ScanResult start, Function<ScanResult, ScanResult> contentInterpreter) {
        super(ScanResultType.IN_PARENTHESES, start);
        int parenthesesLevel = start.getParenthesisLevel();
        ScanResult currentNode = start;
        ScanResult priorNode;
        for (priorNode = currentNode.locatePriorToNextInterpretable(); priorNode != null && priorNode.getNext() != null
                && priorNode.getNext().getParenthesisLevel() >= parenthesesLevel; priorNode = (currentNode == null
                        ? null
                        : currentNode.locatePriorToNextInterpretable())) {
            currentNode = contentInterpreter.apply(priorNode.getNext());
            if (currentNode instanceof CommaSeparatedList) {
                ((CommaSeparatedList) currentNode).setParentIsParentheses();
            }
            priorNode.setNext(currentNode);
        }

        if (priorNode != null) {
            if (priorNode.getNext() == null) {
                setNext(null);
            } else {
                currentNode = priorNode.getNext();
                setNext(currentNode.getNext());
                currentNode.setNext(null);
            }
        } else {
            setNext(null);
        }
        log.debug(() -> this.toString());
    }

    /**
     * @see SrcNode#getParenthesisLevel()
     *
     * @return int the parentheses level AFTER the closing parenthesis
     */
    @Override
    public int getParenthesisLevel() {
        /*
         * The star node (the opening parentesis) returns the parenthesis level inside the InPerentheses object. But
         * getParenthesisLevel() should return the situation AFTER the object, which is one less.
         */
        return super.getParenthesisLevel() - 1;
    }

    /**
     * @see ScanResult#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderMultiLines beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        RenderMultiLines renderResult = getCachedRenderResult(formatContext, parentResult, config);
        if (renderResult != null) {
            return renderResult;
        }

        /*
         * See if we can place a single line result directly after the parentResult
         */
        int singleLineLength = getSingleLineWidth(config);
        FormatContext context = formatContext.clone();
        int parentPosition = 0;
        if (parentResult != null) {
            parentPosition = parentResult.getPosition();
        }
        if (singleLineLength > 0 && parentPosition + singleLineLength <= config.getLineWidth().getValue()) {
            /*
             * The result should fit on a single line
             */
            context.setAvailableWidth(config.getLineWidth().getValue() - parentPosition - 2);
            renderResult = new RenderMultiLines(this, context, parentResult);
            renderResult.addRenderResult(new RenderItem("(", RenderItemType.CHARACTER), formatContext);
            beautifyContent(renderResult, context, config);
            renderResult.addRenderResult(new RenderItem(")", RenderItemType.CHARACTER), formatContext);
            if (renderResult.getHeight() <= 1) {
                return cacheRenderResult(renderResult, formatContext, parentResult);
            }
        }

        int indentBase = 0;
        if (parentResult != null) {
            indentBase = parentResult.getIndentBase();
        }
        /*
         * Keep the opening parenthesis before the content
         */
        RenderMultiLines afterParentAttempt = null;
        if (formatContext.getCommaSeparatedListGrouping().isMultilineOpeningParenBeforeArgument().booleanValue()) {
            int indent = 0;
            switch (formatContext.getCommaSeparatedListGrouping().getIndent().getValue()) {
            case UNDER_FIRST_ARGUMENT:
                /*
                 * With the UNDER_FIRST_ARGUMENT setting active, first an attempt is made to put the result after the
                 * parentResult. Then a fall-through occurs to the DOUBLE_INDENTED case and the result is rendered as
                 * double indented. When that has finished, the two results are compared and the best fit will be
                 * returned.
                 */
                afterParentAttempt = new RenderMultiLines(this, context, parentResult).setIndentBase(parentPosition)
                        .setIndent(2); // paren plus whitespace
                afterParentAttempt.addRenderResult(new RenderItem("(", RenderItemType.CHARACTER), formatContext);
                afterParentAttempt.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
                context.setAvailableWidth(config.getLineWidth().getValue() - afterParentAttempt.getPosition());
                beautifyContent(afterParentAttempt, context, config);
                if (afterParentAttempt.getHeight() > 1) {
                    if (formatContext.getCommaSeparatedListGrouping().isMultilineClosingParenOnNewLine()) {
                        afterParentAttempt.addLine(Util.nSpaces(parentPosition));
                    } else {
                        afterParentAttempt.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE),
                                formatContext);
                    }
                }
                afterParentAttempt.addRenderResult(new RenderItem(")", RenderItemType.CHARACTER), formatContext);
                // fall through
            case DOUBLE_INDENTED:
                indent += config.getStandardIndent();
                // fall through
            case INDENTED:
                indent += config.getStandardIndent();
                if (indentBase + indent < parentPosition) {
                    renderResult = new RenderMultiLines(this, context, parentResult).setIndent(indent);
                    renderResult.addLine();
                    renderResult.addRenderResult(new RenderItem("(", RenderItemType.CHARACTER), formatContext);
                    renderResult.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
                    context.setAvailableWidth(config.getLineWidth().getValue() - renderResult.getPosition());
                    beautifyContent(renderResult, context, config);
                    if (renderResult.getHeight() > 1) {
                        if (formatContext.getCommaSeparatedListGrouping().isMultilineClosingParenOnNewLine()) {
                            renderResult.addLine(Util.nSpaces(indentBase + indent));
                        } else {
                            renderResult.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
                        }
                    }
                    renderResult.addRenderResult(new RenderItem(")", RenderItemType.CHARACTER), formatContext);
                    if (afterParentAttempt != null && afterParentAttempt.getWidth() <= config.getLineWidth().getValue()
                            && (afterParentAttempt.getHeight() < renderResult.getHeight()
                                    || (afterParentAttempt.getHeight() == renderResult.getHeight()
                                            && afterParentAttempt.getWidth() > renderResult.getWidth()))) {
                        renderResult = afterParentAttempt;
                    }
                    return cacheRenderResult(renderResult, formatContext, parentResult);
                }
                break;
            default:
                break;
            }
        }

        renderResult = new RenderMultiLines(this, context, parentResult);
        renderResult.addRenderResult(new RenderItem("(", RenderItemType.CHARACTER), formatContext);
        int indent = 0;
        switch (formatContext.getCommaSeparatedListGrouping().getIndent().getValue()) {
        case DOUBLE_INDENTED:
            indent += config.getStandardIndent();
            // fall through
        case INDENTED:
            indent += config.getStandardIndent();
            renderResult.setIndent(indent);
            renderResult.addLine();
            break;
        case UNDER_FIRST_ARGUMENT:
            renderResult.setIndent(parentPosition - renderResult.getIndentBase());
            renderResult.addLine();
            break;
        default:
            break;
        }
        beautifyContent(renderResult, context, config);
        if (formatContext.getCommaSeparatedListGrouping().isMultilineClosingParenOnNewLine()) {
            renderResult.addLine();
        } else {
            renderResult.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
        }
        renderResult.addRenderResult(new RenderItem(")", RenderItemType.CHARACTER), formatContext);
        return cacheRenderResult(renderResult, formatContext, parentResult);
    }

    /**
     * Formats whatever is between the parentheses
     *
     * @param formatContext
     *            The FormatContext that helps rendering
     * @param config
     *            The FormatConfiguration that specifies how to format
     * @return RenderMultiLines The formatted content
     */
    private void beautifyContent(RenderMultiLines contentResult, FormatContext formatContext,
            FormatConfiguration config) {
        for (ScanResult node = getStartScanResult().getNextNonWhitespace(); node != null
                && !node.isEof(); node = (node == null ? null : node.getNext())) {
            if (node.getNext() != null) {
                contentResult.addRenderResult(node.beautify(formatContext, contentResult, config), formatContext);
            } else {
                // Just include the rest of the string.
                if (!ScanResultType.CLOSING_PARENTHESIS.equals(node.getType())) {
                    contentResult.addRenderResult(new RenderItem(node.toString(), RenderItemType.LITERAL),
                            formatContext);
                }
                break;
            }
        }
        contentResult.removeTrailingSpaces();
    }

    /**
     * @see ScanResult#getSingleLineWidth(FormatConfiguration)
     */
    @Override
    public int getSingleLineWidth(FormatConfiguration config) {
        if (singleLineWidth != 0) {
            return singleLineWidth;
        }
        int elementWidth;
        singleLineWidth = 2; // open and close paren
        for (ScanResult element = this.getStartScanResult(); element != null; element = element
                .getNextNonWhitespace()) {
            elementWidth = element.getSingleLineWidth(config);
            if (elementWidth < 0) {
                singleLineWidth = -1;
                break;
            }
            singleLineWidth += elementWidth;
        }
        return singleLineWidth;
    }

}
