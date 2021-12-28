/*
 * Copyright (c) Splendid Data Product Development B.V. 2020 - 2021
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

import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.BeforeOrAfterType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CommaSeparatedListGroupingType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CommaSeparatedListIndentOption;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
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

    private CommaSeparatedListGroupingType commaSeparatedListGrouping;

    private int cacheParentPosition = 0;
    private RenderMultiLines cachedResult;

    /**
     * Constructor that assumes that the content is a comma separated list of which the content is to be interpreted by
     * {@link PostgresInputReader#interpretPlpgsqlStatementStart(ScanResult)}.
     * 
     * @param start
     *            The opening parenthesis
     */
    public InParentheses(ScanResult start) {
        this(start, contentNode -> CommaSeparatedList.withArbitraryEnd(contentNode,
                innerNode -> PostgresInputReader.interpretStatementBody(innerNode), innerNode -> false));
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
     * @see ScanResult#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderMultiLines beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        int parentPosition = 0;
        if (parentResult != null) {
            parentPosition = parentResult.getPosition();
        }
        if (cachedResult != null && cacheParentPosition == parentPosition) {
            log.debug(() -> "beautify cachedResult= " + cachedResult.beautify());
            return cachedResult.clone();
        }
        if (commaSeparatedListGrouping == null) {
            commaSeparatedListGrouping = formatContext.getCommaSeparatedListGrouping();
        }
        cacheParentPosition = parentPosition;
        int availableSpace = formatContext.getAvailableWidth();
        int standardIndent = FormatContext.indent(true).length();
        int indent = 0;
        switch (commaSeparatedListGrouping.getIndent().getValue()) {
        case DOUBLE_INDENTED:
            indent = standardIndent * 2;
            break;
        case INDENTED:
            indent = standardIndent;
            break;
        case UNDER_FIRST_ARGUMENT:
            if (parentResult != null) {
                indent = parentPosition;
            }
            break;
        default:
            break;
        }

        RenderMultiLines myResult = new RenderMultiLines(this, formatContext) {

            /**
             * @see com.splendiddata.pgcode.formatter.internal.RenderMultiLines#getPosition()
             */
            @Override
            public int getPosition() {
                int position = super.getPosition();
                if (parentResult != null) {
                    position += parentResult.getPosition();
                }
                return position;
            }

        }.setIndent(indent).setOverrideIndent();

        RenderMultiLines contentResult = new RenderMultiLines(this, formatContext).setIndent(0);
        beautifyContent(contentResult, formatContext, config);
        if (contentResult.getHeight() <= 1 && contentResult.getWidth() <= availableSpace) {
            formatContext.setAvailableWidth(availableSpace);
            myResult.addRenderResult(new RenderItem("(", RenderItemType.CHARACTER), formatContext);
            beautifyContent(myResult, formatContext, config);
            myResult.addRenderResult(new RenderItem(")", RenderItemType.CHARACTER), formatContext);
            cachedResult = myResult.clone();
            log.debug(() -> "beautify1 " + this.toString().replace("\n", " ").replaceAll("\\s+", " ") + "\n=\n"
                    + myResult.beautify());
            return myResult;
        }
        if (CommaSeparatedListIndentOption.UNDER_FIRST_ARGUMENT
                .equals(commaSeparatedListGrouping.getIndent().getValue())
                && contentResult.getWidth() + indent <= availableSpace) {
            formatContext.setAvailableWidth(availableSpace);
            myResult.addRenderResult(new RenderItem("(", RenderItemType.CHARACTER), formatContext);
            myResult.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
            myResult.setIndent(indent + 2);
            beautifyContent(myResult, formatContext, config);
            myResult.setIndent(indent);
            addMultilineClosingParenthesis(formatContext, myResult);
            cachedResult = myResult.clone();
            log.debug(() -> "beautify2 " + this.toString().replace("\n", " ").replaceAll("\\s+", " ") + "\n=\n"
                    + myResult.beautify());
            return myResult;
        }
        if (commaSeparatedListGrouping.isMultilineOpeningParenBeforeArgument().booleanValue()) {
            if (parentResult != null && !parentResult.isLastNonWhiteSpaceEqualToLinefeed()) {
                myResult.addWhiteSpace(); // Trick to make myResult to accept the linefeed
                myResult.addLine();
            }
            myResult.addRenderResult(new RenderItem("(", RenderItemType.CHARACTER), formatContext);
            myResult.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
            myResult.setIndent(indent + 2);
            beautifyContent(myResult, formatContext, config);
        } else {
            myResult.addRenderResult(new RenderItem("(", RenderItemType.CHARACTER), formatContext);
            myResult.setIndent(indent);
            myResult.addLine();
            beautifyContent(myResult, formatContext, config);
        }
        myResult.setIndent(indent);
        addMultilineClosingParenthesis(formatContext, myResult);

        formatContext.setAvailableWidth(availableSpace);

        cachedResult = myResult.clone();
        log.debug(() -> "beautify3 " + this.toString().replace("\n", " ").replaceAll("\\s+", " ") + "\n=\n"
                + myResult.beautify());
        return myResult;
    }

    /**
     * Formats whatever is between the parentheses
     *
     * @param formatContext
     *            The FormatContext that helps rendering
     * @param config
     *            The FormatConfiguration that specifies how to format
     * @return RenderMultiLines
     */
    private void beautifyContent(RenderMultiLines contentResult, FormatContext formatContext,
            FormatConfiguration config) {
        for (ScanResult node = getStartScanResult().getNextNonWhitespace(); node != null && !node.isEof(); node = (node == null ? null
                : node.getNext())) {
            if (node.getNext() != null) {
                contentResult.addRenderResult(node.beautify(formatContext, contentResult, config), formatContext);
            } else {
                // Just include the rest of the string.
                if (!ScanResultType.CLOSING_PARENTHESIS.equals(node.getType())) {
                    contentResult.addRenderResult(new RenderItem(node.toString(), RenderItemType.LITERAL), formatContext);
                }
                break;
            }
        }
        contentResult.removeTrailingSpaces();
    }

    /**
     * Adds the closing parenthesis to a multiline render result.
     * <p>
     * The closing parenthesis may be added at the end of the last line of the content or on a new line
     *
     * @param formatContext
     *            The context that will provide the CommaSeparatedListGrouping that tells where to put the closing paren
     * @param addTo
     *            The RenderMultiLines to which the closing paren is to be added
     */
    private void addMultilineClosingParenthesis(FormatContext formatContext, RenderMultiLines addTo) {
        if (formatContext.getCommaSeparatedListGrouping().isMultilineClosingParenOnNewLine().booleanValue()) {
            switch (commaSeparatedListGrouping.getIndent().getValue()) {
            case DOUBLE_INDENTED:
                if (BeforeOrAfterType.BEFORE.equals(formatContext.getCommaSeparatedListGrouping().getCommaBeforeOrAfter())) {
                    // Align the closing paren with the leading commas
                    addTo.addLine(FormatContext.indent(true) + FormatContext.indent(true));
                } else {
                    // Emphatically close the list by exdenting it
                    addTo.addLine(FormatContext.indent(true));
                }
                break;
            case INDENTED:
                addTo.addLine(FormatContext.indent(true));
                break;
            default:
                addTo.addLine();
                break;
            }
            addTo.addRenderResult(new RenderItem(")", RenderItemType.CHARACTER), formatContext);
        } else {
            addTo.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
            addTo.addRenderResult(new RenderItem(")", RenderItemType.CHARACTER), formatContext);
        }
    }
}
