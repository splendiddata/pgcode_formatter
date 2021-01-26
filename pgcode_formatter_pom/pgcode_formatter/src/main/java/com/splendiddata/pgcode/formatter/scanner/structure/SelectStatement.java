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

import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.IntegerValueOption;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Implementation of a select statement (or values statement)
 * 
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class SelectStatement extends SrcNode implements WantsNewlineBefore {
    private static final Logger log = LogManager.getLogger(SelectStatement.class);

    private InParentheses distinctList;
    private CommaSeparatedList targetList;
    private IntoClauseNode intoClause;

    /**
     * Constructor
     *
     * @param scanResult
     *            The word SELECT or VALUES that starts this statement
     */
    public SelectStatement(ScanResult scanResult) {
        this(scanResult, node -> false);
    }

    /**
     * Constructor
     * <p>
     * The select statement is finished when:
     * <ul>
     * <li>ScanResult node.getNext() returns null
     * <li>The parentheses level drops below the parentheses level at the start
     * <li>isComplete returns true
     * <li>A semicolon is passed
     * </ul>
     *
     * @param scanResult
     *            The word SELECT or VALUES that starts this statement
     * @param isComplete
     *            A Predicate&lt;ScanResult&gt; that indicates that the list for ScanResults has reached a node beyond
     *            the select statement.
     */
    public SelectStatement(ScanResult scanResult, Predicate<ScanResult> isComplete) {
        super(ScanResultType.SELECT_STATEMENT, new IdentifierNode(scanResult));
        assert "select".equalsIgnoreCase(scanResult.getText().toLowerCase()) || "values".equalsIgnoreCase(
                scanResult.getText().toLowerCase()) : "A SelectStatement must start witht eh word SELECT, not with "
                        + scanResult;
        log.trace(() -> "Construct " + scanResult + " statement");

        /*
         * A select statement may be wrapped in parentheses
         */
        int parenthesesLevel = scanResult.getParenthesisLevel();

        ScanResult lastInterpreted = getStartScanResult();
        ScanResult priorNode = lastInterpreted.locatePriorToNextInterpretable();
        ScanResult currentNode = priorNode.getNext();
        if ("select".equalsIgnoreCase(lastInterpreted.toString())) {
            if (currentNode != null && currentNode.is(ScanResultType.IDENTIFIER)) {
                if ("all".equalsIgnoreCase(currentNode.toString())) {
                    // SELECT ALL form 
                    lastInterpreted = new IdentifierNode(currentNode);
                    priorNode.setNext(lastInterpreted);
                    priorNode = lastInterpreted.locatePriorToNextInterpretable();
                } else if ("distinct".equalsIgnoreCase(currentNode.toString())) {
                    // SELECT DISTINCT from
                    lastInterpreted = new IdentifierNode(currentNode);
                    priorNode.setNext(lastInterpreted);
                    priorNode = lastInterpreted.locatePriorToNextInterpretable();
                    currentNode = priorNode.getNext();
                    if (currentNode != null && currentNode.is(ScanResultType.IDENTIFIER)
                            && "on".equalsIgnoreCase(currentNode.toString())) {
                        // SELECT DISTINCT ON (col_1, col2, ...) form
                        lastInterpreted = new IdentifierNode(currentNode);
                        priorNode.setNext(lastInterpreted);
                        priorNode = lastInterpreted.locatePriorToNextInterpretable();
                        currentNode = priorNode.getNext();
                        if (currentNode != null && currentNode.is(ScanResultType.OPENING_PARENTHESIS)) {
                            distinctList = new InParentheses(currentNode,
                                    node -> CommaSeparatedList.withArbitraryEnd(node,
                                            intelrnalNode -> PostgresInputReader.interpretStatementBody(intelrnalNode),
                                            intelrnalNode -> false));
                            lastInterpreted = distinctList;
                            priorNode.setNext(lastInterpreted);
                            priorNode = lastInterpreted.locatePriorToNextInterpretable();
                            currentNode = priorNode.getNext();
                        }
                    }
                }
            }
            /*
             * The target list
             */
            targetList = CommaSeparatedList.withArbitraryEnd(priorNode.getNext(),
                    node -> PostgresInputReader.interpretStatementBody(node), node -> {
                        if (isComplete.test(node)) {
                            return true;
                        }
                        if (!node.is(ScanResultType.IDENTIFIER)) {
                            return false;
                        }
                        switch (node.toString().toLowerCase()) {
                        case "from":
                        case "where":
                        case "group":
                        case "having":
                        case "window":
                        case "union":
                        case "intersect":
                        case "except":
                        case "order":
                        case "limit":
                        case "offset":
                        case "fetch":
                        case "for":
                        case "into":
                            return true;
                        default:
                            return false;
                        }
                    });
            /*
             * Make sure a column name or alias is not interpreted as keyword
             */
            if (targetList != null) {
                targetList.getElements().stream().forEach(element -> {
                    ScanResult node;
                    for (node = element.getStartScanResult(); node != null; node = node.getNext()) {
                        if (node instanceof IdentifierNode && node.getNextInterpretable() == null) {
                            ((IdentifierNode) node).setNotKeyword(true);
                            break;
                        }
                    }
                });
            }
            lastInterpreted = targetList;
            priorNode.setNext(lastInterpreted);
            if (log.isTraceEnabled()) {
                log.trace("target list = <" + lastInterpreted + ">");
            }
        } else if (currentNode != null && "values".equalsIgnoreCase(lastInterpreted.toString())
                && currentNode.is(ScanResultType.OPENING_PARENTHESIS)) {
            lastInterpreted = new InParentheses(currentNode, node -> PostgresInputReader.interpretStatementBody(node));
            priorNode.setNext(lastInterpreted);
        }

        /*
         * The rest of the statement
         */
        if (lastInterpreted != null) {
            for (priorNode = lastInterpreted.locatePriorToNextInterpretable(); priorNode
                    .getNext() != null; priorNode = lastInterpreted.locatePriorToNextInterpretable()) {
                currentNode = priorNode.getNext();
                if (currentNode.isStatementEnd() || currentNode.getParenthesisLevel() < parenthesesLevel
                        || isComplete.test(currentNode)) {
                    break;
                }
                if (currentNode.is(ScanResultType.IDENTIFIER)) {
                    switch (currentNode.toString().toLowerCase()) {
                        case "from":
                            lastInterpreted = new FromClause(currentNode);
                            if (log.isTraceEnabled()) {
                                log.trace("from list = <" + lastInterpreted + ">");
                            }
                            break;
                        case "where":
                            lastInterpreted = new WhereConditionNode(currentNode);
                            if (log.isTraceEnabled()) {
                                log.trace("where condition = <" + lastInterpreted + ">");
                            }
                            break;
                        case "group":
                            lastInterpreted = new GroupByClause(currentNode);
                            if (log.isTraceEnabled()) {
                                log.trace("group by list = <" + lastInterpreted + ">");
                            }
                            break;
                        case "having":
                            lastInterpreted = new HavingClause(currentNode);
                            if (log.isTraceEnabled()) {
                                log.trace("having clause = <" + lastInterpreted + ">");
                            }
                            break;
                        case "window":
                            lastInterpreted = new WindowClause(currentNode);
                            if (log.isTraceEnabled()) {
                                log.trace("window clause = <" + lastInterpreted + ">");
                            }
                            break;
                        case "union":
                        case "intersect":
                        case "except":
                            lastInterpreted = new UnionClauseNode(currentNode);
                            if (log.isTraceEnabled()) {
                                log.trace("union clause = <" + lastInterpreted + ">");
                            }
                            break;
                        case "order":
                            lastInterpreted = new OrderByClause(currentNode);
                            if (log.isTraceEnabled()) {
                                log.trace("order by list = <" + lastInterpreted + ">");
                            }
                            break;
                        case "limit":
                            lastInterpreted = new LimitClause(currentNode);
                            if (log.isTraceEnabled()) {
                                log.trace("limit clause = <" + lastInterpreted + ">");
                            }
                            break;
                        case "offset":
                            lastInterpreted = new OffsetClause(currentNode);
                            if (log.isTraceEnabled()) {
                                log.trace("offset clause = <" + lastInterpreted + ">");
                            }
                            break;
                        case "fetch":
                            lastInterpreted = new FetchClause(currentNode);
                            if (log.isTraceEnabled()) {
                                log.trace("fetch clause = <" + lastInterpreted + ">");
                            }
                            break;
                        case "into":
                            intoClause = new IntoClauseNode(currentNode);
                            lastInterpreted = intoClause;
                            if (log.isTraceEnabled()) {
                                log.trace("into clause = <" + lastInterpreted + ">");
                            }
                            break;
                        case "for":
                            lastInterpreted = new ForUpdateClause(currentNode);
                            if (log.isTraceEnabled()) {
                                log.trace("for clause = <" + lastInterpreted + ">");
                            }
                            break;
                        default:
                            lastInterpreted = PostgresInputReader.interpretStatementBody(currentNode);
                            if (log.isTraceEnabled()) {
                                log.trace("something else = " + lastInterpreted.getClass().getSimpleName() + " <"
                                        + lastInterpreted + ">");
                            }
                            break;
                    }
                    priorNode.setNext(lastInterpreted);
                } else {
                    lastInterpreted = PostgresInputReader.interpretStatementBody(currentNode);
                    if (log.isTraceEnabled()) {
                        log.trace("not an identifier = " + lastInterpreted.getClass().getSimpleName() + " <"
                                + lastInterpreted + ">");
                    }
                    priorNode.setNext(lastInterpreted);
                }
            }
        }

        if (currentNode != null && currentNode.is(ScanResultType.SEMI_COLON) && !isComplete.test(currentNode)) {
            lastInterpreted = new SemiColonNode(currentNode);
            priorNode.setNext(lastInterpreted);
        }
        if (lastInterpreted != null) {
            setNext(lastInterpreted.getNext());
            lastInterpreted.setNext(null);
        } else {
            setNext(null);
        }
        log.debug(() -> "constructed select statement = " + this);
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        if (parentResult != null && config.getQueryConfig().isMajorKeywordsOnSeparateLine().booleanValue()
                && !parentResult.isLastNonWhiteSpaceEqualToLinefeed()) {
            parentResult.addLine();
        }
        int availableWidth = formatContext.getAvailableWidth();
        boolean majorKeywordsOnSeparateLine = config.getQueryConfig().isMajorKeywordsOnSeparateLine().booleanValue();
        IntegerValueOption queryWidthSetting = config.getQueryConfig().getMaxSingleLineQuery();
        boolean doIndent = config.getQueryConfig().isIndent().booleanValue();
        String standardIndent = FormatContext.indent(true);

        /*
         * First try to render it on the current line.
         */
        FormatContext targetListContext = new FormatContext(config, formatContext);
        targetListContext.setCommaSeparatedListGrouping(config.getTargetListGrouping());
        if (!majorKeywordsOnSeparateLine) {
            RenderMultiLines result = new RenderMultiLines(this, formatContext);
            int maxWidth = availableWidth;
            if (maxWidth > queryWidthSetting.getValue()) {
                maxWidth = queryWidthSetting.getValue();
            }
            formatContext.setAvailableWidth(maxWidth);
            for (ScanResult current = getStartScanResult(); current != null; current = current.getNext()) {
                if (current == targetList || current == distinctList) {
                    result.addRenderResult(
                            current.beautify(targetListContext.setAvailableWidth(availableWidth - result.getPosition()),
                                    result, config),
                            formatContext);
                } else {
                    result.addRenderResult(
                            current.beautify(formatContext.setAvailableWidth(availableWidth - result.getPosition()),
                                    result, config),
                            formatContext);
                }
            }
            if (result.getHeight() == 1 && result.getWidth() <= maxWidth) {
                formatContext.setAvailableWidth(availableWidth);
                return result;
            }
        }

        /*
         * Rendering on a single line didn't work out, so let's render multiline
         */
        RenderMultiLines result = new RenderMultiLines(this, formatContext);
        if (!doIndent) {
            result.setIndent(0);
        }

        ScanResult current = getStartScanResult();
        if (config.getQueryConfig().isMajorKeywordsOnSeparateLine().booleanValue()) {
            if ("select".equalsIgnoreCase(current.toString())) {
                ScanResult renderUntil = current;
                ScanResult peekNext = current.getNextInterpretable();
                if (peekNext != null && peekNext.is(ScanResultType.IDENTIFIER)) {
                    if ("all".equalsIgnoreCase(peekNext.toString())) {
                        renderUntil = peekNext;
                    } else if ("distinct".equalsIgnoreCase(peekNext.toString())) {
                        renderUntil = peekNext;
                        peekNext = peekNext.getNextInterpretable();
                        if (peekNext != null && peekNext.is(ScanResultType.IDENTIFIER)
                                && "on".equalsIgnoreCase(peekNext.toString())) {
                            renderUntil = peekNext;
                        }
                    }
                }
                renderUntil = renderUntil.getNext();
                for (; current != renderUntil; current = current.getNext()) {
                    result.addRenderResult(current.beautify(formatContext, result, config), formatContext);
                }
            } else {
                result.addRenderResult(current.beautify(formatContext, result, config), formatContext);
                current = current.getNext();
            }
            result.addLine();
        }
        for (; current != null; current = current.getNext()) {
            if (current == targetList || current == distinctList) {
                result.addRenderResult(
                        current.beautify(targetListContext.setAvailableWidth(availableWidth - result.getPosition()),
                                result, config),
                        formatContext);
            } else if (current instanceof WantsNewlineBefore) {
                result.addLine();
                if (doIndent) {
                    result.addRenderResult(
                            current.beautify(formatContext.setAvailableWidth(availableWidth - standardIndent.length()),
                                    result, config),
                            formatContext);
                } else {
                    result.addRenderResult(
                            current.beautify(formatContext.setAvailableWidth(availableWidth - result.getPosition()),
                                    result, config),
                            formatContext);
                }
            } else {
                result.addRenderResult(
                        current.beautify(formatContext.setAvailableWidth(availableWidth - result.getPosition()), result,
                                config),
                        formatContext);
            }
        }

        formatContext.setAvailableWidth(availableWidth);
        return result;
    }
}
