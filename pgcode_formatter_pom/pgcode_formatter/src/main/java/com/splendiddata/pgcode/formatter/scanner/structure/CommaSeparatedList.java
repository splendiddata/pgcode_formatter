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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.BeforeOrAfterType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CommaSeparatedListGroupingType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CommaSeparatedListIndentOption;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CommaSeparatedListIndentType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.IntegerValueOption;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.internal.Util;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A comma separated list
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class CommaSeparatedList extends SrcNode {
    private static final Logger log = LogManager.getLogger(CommaSeparatedList.class,
            StringFormatterMessageFactory.INSTANCE);

    private List<ListElement> elements;
    private CommaSeparatedListGroupingType commaSeparatedListGrouping;

    private int maxElementHeight = 0;
    private int maxElementWidth = 0;
    private int totalElementWidth = 0;
    private List<RenderResult> renderedElements = null;

    /**
     * Constructor Please invoke via {@link #ofDistinctElementTypes(ScanResult, Function)}
     *
     * @param content
     *            The ScanResult node that starts the content
     * @param next
     *            The first ScanResult after this CommaSeparatedList. This node will be returned by the
     *            {@link #getNext()} method.
     */
    private CommaSeparatedList(ScanResult content, ScanResult next) {
        super(ScanResultType.COMMA_SEPARATED_LIST, content);
        setNext(next);
    }

    /**
     * Constructs a comma separated list of well defined single elements. The elements are constructed using the
     * elementConstructor
     *
     * @param startNode
     *            ScanResult that starts the first element.
     * @param elementConstructor
     *            Function&lt;ScanResult, SrcNode&gt; The function that converts the SrcNodes between the commas to a
     *            list element
     * @return CommaSeparatedList
     */
    public static CommaSeparatedList ofDistinctElementTypes(ScanResult startNode,
            Function<ScanResult, SrcNode> elementConstructor) {
        SrcNode firstElement = elementConstructor.apply(startNode);
        SrcNode lastElement = firstElement;
        ScanResult priorNode = firstElement.locatePriorToNextInterpretable();
        ScanResult currentNode = priorNode.getNext();
        for (priorNode = lastElement.locatePriorToNextInterpretable();; priorNode = lastElement
                .locatePriorToNextInterpretable()) {
            currentNode = priorNode.getNext();
            if (currentNode == null || !currentNode.is(ScanResultType.CHARACTER)
                    || !",".equals(currentNode.toString())) {
                break;
            }
            priorNode = currentNode.locatePriorToNextInterpretable();
            currentNode = priorNode.getNext();
            if (currentNode == null) {
                break;
            }
            lastElement = elementConstructor.apply(currentNode);
            priorNode.setNext(lastElement);
        }
        CommaSeparatedList result = new CommaSeparatedList(firstElement, lastElement.getNext());
        lastElement.setNext(null);
        log.debug(() -> result);
        return result;
    }

    /**
     * constructs a CommaSeparatedList using the contentInterpreter to interpret the content of the list elements. The
     * list is considered ended when the isComplete predicate returns true.
     * <p>
     * The isComplete predicate will be invoked before a node to test is interpreted. The node that caused the
     * isComplete predicate to return true will be returned by the getNext() method of the constructed
     * CommaSeparatedList. For example the implementation of the target list of a select statement would look somewhat
     * like:
     * 
     * <pre>
     * <code>
     *     CommaSeparatedList targets = CommaSeparatedList.withArbitraryEnd(curentNode
     *                                      , node-&gt;PostgresInputReader.interpretStatementBody(node)
     *                                      , node-&gt;{    if (!node.is(ScanResultType.IDENTIFIER)) {
     *                                                       return false;
     *                                                   }
     *                                                   switch (node.toString().toLowerCase()) {
     *                                                   case "from":
     *                                                   case "where":
     *                                                   case "group":
     *                                                   case "having":
     *                                                   case "window":
     *                                                   case "union":
     *                                                   case "intersect":
     *                                                   case "except":
     *                                                   case "order":
     *                                                   case "limit":
     *                                                   case "offset":
     *                                                   case "fetch":
     *                                                   case "for":
     *                                                   case "into":
     *                                                       return true;
     *                                                   default:
     *                                                       return false;
     *                                                   }
     *                                              });                                                                 
     * </code>
     * </pre>
     * <p>
     * end of file, a semicolon or a closing parenthesis that makes the parentheses level drop below the parentheses
     * level of the startNode will always end the CommaSeparatedList. So even if the isComnplete lambda always returns
     * true, the list will still come to an end.
     * <p>
     * The startNode is always considered part of the list. So on that node the predicate will not be executed.
     * 
     * @param startNode
     *            The ScanResult that will start the first element of the list
     * @param contentInterpreter
     *            Function&lt;ScanResult, ScanResult&gt; lambda to interpret the content of the list elements. Will be
     *            invoked on every effective node until the list is complete
     * @param isComplete
     *            Predicate&lt;ScanResult&gt; lambda that ends the comma separated list when it returns true. The node
     *            that causes the true result will be outside the list and will be returned by the
     *            CommaSeparatedList.getNext() function.
     * @return CommaSeparatedList The comma separated list
     */
    public static CommaSeparatedList withArbitraryEnd(ScanResult startNode,
            Function<ScanResult, ScanResult> contentInterpreter, Predicate<ScanResult> isComplete) {
        int parenthesesLevel = startNode.getParenthesisLevel();
        if (parenthesesLevel > 0 && startNode.is(ScanResultType.OPENING_PARENTHESIS)) {
            /*
             * An opening parenthesis increments its parentheses level before it is interpreted
             */
            parenthesesLevel--;
        }
        ScanResult firstElement = contentInterpreter.apply(startNode);
        ScanResult lastInterpreted = firstElement;
        ScanResult priorNode;
        ScanResult currentNode;

        if (firstElement != null) {
            for (priorNode = firstElement.locatePriorToNextInterpretable(); ; priorNode = currentNode
                    .locatePriorToNextInterpretable()) {
                currentNode = priorNode.getNext();
                if (currentNode == null || currentNode.isStatementEnd()
                        || currentNode.getParenthesisLevel() < parenthesesLevel || isComplete.test(currentNode)) {
                    break;
                }
                currentNode = contentInterpreter.apply(currentNode);
                priorNode.setNext(currentNode);
                if (currentNode.getType().isInterpretable()) {
                    lastInterpreted = currentNode;
                }
            }
            CommaSeparatedList result = new CommaSeparatedList(firstElement, lastInterpreted.getNext());
            lastInterpreted.setNext(null);
            log.debug(() -> result);
            return result;
        }
        return null;
    }

    /**
     * @see ScanResult#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderMultiLines beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {

        if (commaSeparatedListGrouping == null) {
            commaSeparatedListGrouping = formatContext.getCommaSeparatedListGrouping();
        }
        IntegerValueOption maxSingleLineLength = commaSeparatedListGrouping.getMaxSingleLineLength();
        IntegerValueOption maxGroupLength = commaSeparatedListGrouping.getMaxLengthOfGroup();
        IntegerValueOption maxElementsPerGroup = commaSeparatedListGrouping.getMaxArgumentsPerGroup();
        CommaSeparatedListIndentType indentValue = commaSeparatedListGrouping.getIndent();
        int indent = 0;
        switch (indentValue.getValue()) {
        case DOUBLE_INDENTED:
            indent = 2 * FormatContext.indent(true).length();
            break;
        case INDENTED:
            indent = FormatContext.indent(true).length();
            break;
        case UNDER_FIRST_ARGUMENT:
        default:
            if (parentResult != null) {
                indent = parentResult.getPosition();
            }
            break;
        }
        int availableWidth = formatContext.getAvailableWidth();
        FormatContext elementContext = new FormatContext(config, formatContext);
        elementContext.setCommaSeparatedListGrouping(config.getCommaSeparatedListGrouping());
        if (BeforeOrAfterType.BEFORE.equals(commaSeparatedListGrouping.getCommaBeforeOrAfter())) {
            elementContext.setAvailableWidth(availableWidth - 2); // room for ", "
        } else {
            elementContext.setAvailableWidth(availableWidth - 1); // room for just a comma
        }
        if (renderedElements == null) {
            renderedElements = new ArrayList<>(getElements().size());
            for (ListElement element : getElements()) {
                renderedElements.add(element.beautify(elementContext, null, config));
            }

            for (RenderResult elementResult : renderedElements) {
                int elementHeight = elementResult.getHeight();
                int elementWidth = elementResult.getWidth();
                if (elementHeight > maxElementHeight) {
                    maxElementHeight = elementHeight;
                }
                if (elementWidth > maxElementWidth) {
                    maxElementWidth = elementWidth;
                }
                totalElementWidth += elementWidth;
            }
        }

        RenderMultiLines result;
        if (maxElementHeight == 1) {
            int totalSingleLineLength = totalElementWidth + 2 * (renderedElements.size() - 1);
            if (renderedElements.size() <= 1
                    || (totalSingleLineLength <= maxSingleLineLength.getValue()
                            && maxSingleLineLength.getWeight().floatValue() > maxGroupLength.getWeight().floatValue()
                            && maxSingleLineLength.getWeight().floatValue() >= indentValue.getWeight().floatValue()
                            && (renderedElements.size() <= maxElementsPerGroup.getValue() || maxSingleLineLength
                                    .getWeight().floatValue() > maxElementsPerGroup.getWeight().floatValue()))
                    || (totalSingleLineLength > maxSingleLineLength.getValue()
                            && totalSingleLineLength <= maxGroupLength.getValue()
                            && maxGroupLength.getWeight().floatValue() >= maxSingleLineLength.getWeight().floatValue()
                            && maxGroupLength.getWeight().floatValue() >= indentValue.getWeight().floatValue()
                            && (renderedElements.size() <= maxElementsPerGroup.getValue() || maxGroupLength.getWeight()
                                    .floatValue() > maxElementsPerGroup.getWeight().floatValue()))) {
                /*
                 * The list is small enough to be put into a single line result
                 */
                result = new RenderMultiLines(this, formatContext).setIndent(indent).setOverrideIndent();
                boolean first = true;
                for (RenderResult element : renderedElements) {
                    if (first) {
                        first = false;
                    } else {
                        result.removeTrailingSpaces();
                        result.addRenderResult(new RenderItem(",", RenderItemType.CHARACTER), formatContext);
                        result.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
                    }
                    result.addRenderResult(element, formatContext);
                }
                log.trace(() -> new StringBuilder().append("single line result <").append(this).append(">\nsettings=")
                        .append(Util.xmlBeanToString(commaSeparatedListGrouping)).append(" result=\n")
                        .append(result.beautify()));
                return result;
            }

            /*
             * All elements could be grouped on a single line, but the line would get too long. So the list will be
             * split up in several lines, but the list elements will be grouped
             */
            if (maxGroupLength.getWeight().floatValue() >= maxElementsPerGroup.getWeight().floatValue()
                    && maxGroupLength.getWeight().floatValue() >= indentValue.getWeight().floatValue()) {
                result = new RenderMultiLines(this, formatContext).setIndent(indent).setOverrideIndent();
                int elementsOnLine = 0;
                for (RenderResult element : renderedElements) {
                    if (elementsOnLine > 0 && (result.getPosition() + 2 + element.getWidth() >= maxGroupLength
                            .getValue()
                            || (maxGroupLength.getWeight().floatValue() == maxElementsPerGroup.getWeight().floatValue()
                                    && elementsOnLine >= maxElementsPerGroup.getValue()))) {
                        nextElementOnNextLine(result, formatContext);
                        elementsOnLine = 0;
                    }
                    if (elementsOnLine != 0) {
                        result.removeTrailingSpaces();
                        result.addRenderResult(new RenderItem(",", RenderItemType.CHARACTER), formatContext);
                        result.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
                    }
                    result.addRenderResult(element, formatContext);
                    elementsOnLine++;
                }
                log.trace(() -> new StringBuilder().append("result group length <").append(this).append(">\nsettings=")
                        .append(Util.xmlBeanToString(commaSeparatedListGrouping)).append(" result=\n")
                        .append(result.beautify()));
                return result;
            }
            if (maxElementsPerGroup.getWeight().floatValue() > maxGroupLength.getWeight().floatValue()
                    && maxElementsPerGroup.getWeight().floatValue() >= indentValue.getWeight().floatValue()) {
                result = new RenderMultiLines(this, formatContext).setIndent(indent).setOverrideIndent();
                int elementsOnLine = 0;
                for (RenderResult element : renderedElements) {
                    if (elementsOnLine >= maxElementsPerGroup.getValue()) {
                        nextElementOnNextLine(result, formatContext);
                        elementsOnLine = 0;
                    }
                    if (elementsOnLine != 0) {
                        result.removeTrailingSpaces();
                        result.addRenderResult(new RenderItem(",", RenderItemType.CHARACTER), formatContext);
                        result.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
                    }
                    result.addRenderResult(element, formatContext);
                    elementsOnLine++;
                }
                log.trace(() -> new StringBuilder().append("result max elements per group <").append(this)
                        .append(">\nsettings=").append(Util.xmlBeanToString(commaSeparatedListGrouping))
                        .append(" result=\n").append(result.beautify()));
                return result;
            }
        }

        /*
         * There are elements that need more that one line. So it is better to start every element on a line of its own.
         */
        result = new RenderMultiLines(this, formatContext).setIndent(indent).setOverrideIndent();
        boolean first = true;
        for (ScanResult element : getElements()) {
            if (first) {
                first = false;
            } else {
                nextElementOnNextLine(result, formatContext);
            }
            result.addRenderResult(element.beautify(formatContext, result, config), formatContext);
        }

        log.trace(() -> new StringBuilder().append("result all elements separate <").append(this).append(">\nsettings=")
                .append(Util.xmlBeanToString(commaSeparatedListGrouping)).append(" result=\n")
                .append(result.beautify()));
        return result;
    }

    /**
     * Places a comma at the end of the current line and adds a line feed or adds a line feed and a comma on the next
     * line, depending on the commaBeforeOrAfter setting of the commaSeparatedListGrouping
     *
     * @param result
     *            The render result that needs a new line and a comma
     * @param formatContext
     *            for rendering
     */
    private void nextElementOnNextLine(RenderMultiLines result, FormatContext formatContext) {
        if (BeforeOrAfterType.BEFORE.equals(commaSeparatedListGrouping.getCommaBeforeOrAfter())) {
            int indent = result.getStandardIndent();
            if (CommaSeparatedListIndentOption.UNDER_FIRST_ARGUMENT
                    .equals(commaSeparatedListGrouping.getIndent().getValue())) {
                result.setIndent(indent - 2);
            }
            result.addLine();
            result.setIndent(indent);
            result.addRenderResult(new RenderItem(",", RenderItemType.CHARACTER), formatContext);
            result.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
        } else {
            result.addRenderResult(new RenderItem(",", RenderItemType.CHARACTER), formatContext);
            result.addLine();
        }
    }

    /**
     * Rebuilds the CommaSeparatedList into a sequence of comma separated ListElements and returns the ListElements as a
     * List
     *
     * @return List&lt;ListElement&gt;
     */
    public List<ListElement> getElements() {
        if (elements == null) {
            elements = new ArrayList<>();
            ScanResult currentNode = new ListElement(getStartScanResult());
            replaceStartScanResult(currentNode);
            elements.add((ListElement) currentNode);
            ScanResult priorNode = currentNode;
            for (currentNode = priorNode.getNext(); currentNode != null; currentNode = currentNode.getNext()) {
                if (currentNode.is(ScanResultType.CHARACTER) && ",".equals(currentNode.toString())) {
                    for (priorNode = currentNode; priorNode.getNext() != null
                            && (priorNode.getNext().is(ScanResultType.WHITESPACE)
                                    || priorNode.getNext().is(ScanResultType.LINEFEED)); priorNode = priorNode
                                            .getNext()) {
                        // Skip whitespace
                    }
                    if (priorNode.getNext() != null) {
                        currentNode = new ListElement(priorNode.getNext());
                        priorNode.setNext(currentNode);
                        elements.add((ListElement) currentNode);
                    }
                }
            }
        }
        log.debug(() -> "getElements() = " + elements);
        return elements;
    }

}
