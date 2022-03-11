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
import java.util.ListIterator;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;

import com.splendiddata.pgcode.formatter.ConfigUtil;
import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.BeforeOrAfterType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CommaSeparatedListIndentType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.IntegerValueOption;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
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

    private int singleLineLength = 0;

    private boolean parentIsParentheses;

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
            for (priorNode = firstElement.locatePriorToNextInterpretable();; priorNode = currentNode
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
        RenderMultiLines renderResult = getCachedRenderResult(formatContext, parentResult, config);
        if (renderResult != null) {
            return renderResult;
        }
        int parentPosition = 0;
        if (parentResult != null) {
            parentPosition = parentResult.getPosition();
        }
        IntegerValueOption maxLineLength = config.getLineWidth();
        IntegerValueOption maxSingleLineLength = formatContext.getCommaSeparatedListGrouping().getMaxSingleLineLength();
        IntegerValueOption maxGroupLength = formatContext.getCommaSeparatedListGrouping().getMaxLengthOfGroup();
        IntegerValueOption maxElementsPerGroup = formatContext.getCommaSeparatedListGrouping()
                .getMaxArgumentsPerGroup();
        if (getElements().size() <= 1 || formatContext.getCommaSeparatedListGrouping().getIndent().getWeight()
                .floatValue() < maxSingleLineLength.getWeight().floatValue()) {
            int singleLineWidth = getSingleLineWidth(config);
            if (singleLineWidth >= 0) {
                boolean singleLineDecision = singleLineWidth + parentPosition <= maxLineLength.getValue();
                float decisionWeight = singleLineDecision ? 0F : maxLineLength.getWeight().floatValue();
                if (maxSingleLineLength.getWeight().floatValue() >= decisionWeight) {
                    if (singleLineWidth > maxSingleLineLength.getValue()) {
                        singleLineDecision = false;
                        decisionWeight = maxSingleLineLength.getWeight().floatValue();
                    } else if (decisionWeight < maxSingleLineLength.getWeight().floatValue()) {
                        singleLineDecision = true;
                        decisionWeight = maxSingleLineLength.getWeight().floatValue();
                    }
                }
                if (maxGroupLength.getWeight().floatValue() >= decisionWeight) {
                    if (singleLineWidth > maxGroupLength.getValue()) {
                        singleLineDecision = false;
                        decisionWeight = maxGroupLength.getWeight().floatValue();
                    } else if (decisionWeight < maxGroupLength.getWeight().floatValue()) {
                        singleLineDecision = true;
                        decisionWeight = maxGroupLength.getWeight().floatValue();
                    }
                }
                if (maxElementsPerGroup.getWeight().floatValue() >= decisionWeight) {
                    if (getElements().size() > maxElementsPerGroup.getValue()) {
                        singleLineDecision = false;
                        decisionWeight = maxElementsPerGroup.getWeight().floatValue();
                    } else if (decisionWeight < maxElementsPerGroup.getWeight().floatValue()) {
                        singleLineDecision = true;
                        decisionWeight = maxElementsPerGroup.getWeight().floatValue();
                    }
                }
                if (singleLineDecision) {
                    renderResult = new RenderMultiLines(this, formatContext, parentResult);
                    boolean first = true;
                    for (ListElement element : getElements()) {
                        if (first) {
                            first = false;
                        } else {
                            renderResult.removeTrailingSpaces();
                            renderResult.addRenderResult(new RenderItem(",", RenderItemType.CHARACTER), formatContext);
                            renderResult.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
                        }
                        renderResult.addRenderResult(element.beautify(formatContext, renderResult, config),
                                formatContext);
                    }
                    if (renderResult.getHeight() <= 1) {
                        return cacheRenderResult(renderResult, formatContext, parentResult);
                    }
                }
            }
        }

        if (formatContext.getCommaSeparatedListGrouping().getIndent().getWeight().floatValue() > maxElementsPerGroup
                .getWeight().floatValue()
                && formatContext.getCommaSeparatedListGrouping().getIndent().getWeight().floatValue() > maxGroupLength
                        .getWeight().floatValue()) {
            /*
             * The indent value is more important then the maxElementsPerGroup value and the maxGroupLength value. So
             * every element should start on a line of its own.
             */
            maxElementsPerGroup = ConfigUtil
                    .copy(formatContext.getCommaSeparatedListGrouping().getMaxArgumentsPerGroup());
            maxElementsPerGroup.setValue(1);
            maxElementsPerGroup.setWeight(Float.MAX_VALUE);
        } else {
            int elementWidth;
            for (ListElement element : getElements()) {
                elementWidth = element.getSingleLineWidth(config);
                if (elementWidth < 0) {
                    /*
                     * At least one element cannot be rendered on a single line, so every element should start on a line
                     * of its own
                     */
                    maxElementsPerGroup = ConfigUtil
                            .copy(formatContext.getCommaSeparatedListGrouping().getMaxArgumentsPerGroup());
                    maxElementsPerGroup.setValue(1);
                    maxElementsPerGroup.setWeight(Float.MAX_VALUE);
                    break;
                }
            }
        }

        int indentBase = 0;
        if (parentResult != null) {
            indentBase = parentResult.getIndentBase();
        }
        int newLinePosition = 0;
        int elementsOnLine = 0;
        CommaSeparatedListIndentType indentValue = formatContext.getCommaSeparatedListGrouping().getIndent();
        if (parentIsParentheses) {
            /*
             * The surrounding InParentheses will decide where the elements will be placed.
             */
            newLinePosition = parentPosition;
        } else {
            switch (indentValue.getValue()) {
            case DOUBLE_INDENTED:
                newLinePosition = indentBase + 2 * config.getStandardIndent();
                break;
            case INDENTED:
                newLinePosition = indentBase + config.getStandardIndent();
                break;
            case UNDER_FIRST_ARGUMENT:
            default:
                newLinePosition = parentPosition;
                break;
            }
        }
        renderResult = new RenderMultiLines(this, formatContext, parentResult).setIndentBase(newLinePosition);
//        if (renderResult.getPosition() > newLinePosition) {
//            renderResult.addLine(Util.nSpaces(newLinePosition));
//        }
        ListIterator<ListElement> it = getElements().listIterator();
        ListElement element = it.next();
        int elementLength = -1;
        int groupLength;
        float decisionWeight;
        while (element != null) {
            elementsOnLine = 1;
            renderResult.addRenderResult(element.beautify(formatContext, renderResult, config), formatContext);
            groupLength = renderResult.getPosition() - newLinePosition;
            element = null;
            while (it.hasNext()) {
                element = it.next();
                elementsOnLine++;
                if (elementsOnLine > maxElementsPerGroup.getValue()
                        && maxElementsPerGroup.getWeight().floatValue() >= maxGroupLength.getWeight().floatValue()) {
                    break;
                }
                decisionWeight = maxElementsPerGroup.getWeight().floatValue();
                elementLength = element.getSingleLineWidth(config);
                if (elementLength < 0) {
                    break;
                }
                groupLength += elementLength + 2;
                if (groupLength > maxGroupLength.getValue()
                        && maxGroupLength.getWeight().floatValue() >= maxElementsPerGroup.getWeight().floatValue()) {
                    break;
                }
                if (decisionWeight < maxGroupLength.getWeight()) {
                    decisionWeight = maxGroupLength.getWeight().floatValue();
                }
                if (groupLength + newLinePosition > maxLineLength.getValue()
                        && maxLineLength.getWeight().floatValue() >= decisionWeight) {
                    break;
                }
                renderResult.addRenderResult(new RenderItem(",", RenderItemType.CHARACTER), formatContext);
                renderResult.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
                renderResult.addRenderResult(element.beautify(formatContext, renderResult, config), formatContext);
                element = null;
            }
            if (element != null) {
                if (BeforeOrAfterType.BEFORE
                        .equals(formatContext.getCommaSeparatedListGrouping().getCommaBeforeOrAfter())) {
                    renderResult.positionAt(newLinePosition - 2);
                    renderResult.addRenderResult(new RenderItem(",", RenderItemType.CHARACTER), formatContext);
                    renderResult.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
                } else {
                    renderResult.positionAfterLastNonWhitespace();
                    renderResult.addRenderResult(new RenderItem(",", RenderItemType.CHARACTER), formatContext);
                    renderResult.positionAt(newLinePosition);
                }
            }
        }
        return cacheRenderResult(renderResult, formatContext, parentResult);
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

    /**
     * @see ScanResult#getSingleLineWidth(FormatConfiguration)
     */
    @Override
    public int getSingleLineWidth(FormatConfiguration config) {
        if (singleLineLength != 0) {
            // calculated before
            return singleLineLength;
        }
        int elementWidth;
        if (getElements().size() == 1) {
            singleLineLength = getElements().get(0).getSingleLineWidth(config);
            return singleLineLength;
        }
        if (config.getCommaSeparatedListGrouping().getMaxArgumentsPerGroup().getWeight().floatValue() >= config
                .getCommaSeparatedListGrouping().getMaxSingleLineLength().getWeight().floatValue()
                && getElements().size() > config.getCommaSeparatedListGrouping().getMaxArgumentsPerGroup().getValue()) {
            singleLineLength = -1;
            return singleLineLength;
        }
        for (ListElement element : getElements()) {
            elementWidth = element.getSingleLineWidth(config);
            if (elementWidth < 0) {
                singleLineLength = -1;
                return singleLineLength;
            }
            singleLineLength += elementWidth;
        }
        if (singleLineLength > 0) {
            singleLineLength += 2 * (getElements().size() - 1); // Room for the commas
        }
        return singleLineLength;
    }

    /**
     * Indicates that the parent result is a {@link com.splendiddata.pgcode.formatter.scanner.structure.InParentheses}
     *
     * @return CommaSeparatedList this
     */
    public CommaSeparatedList setParentIsParentheses() {
        parentIsParentheses = true;
        return this;
    }
}
