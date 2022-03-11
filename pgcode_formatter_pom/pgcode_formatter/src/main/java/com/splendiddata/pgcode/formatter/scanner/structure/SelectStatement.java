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

import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.ConfigUtil;
import com.splendiddata.pgcode.formatter.FormatConfiguration;
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

    private int singleLineLength;

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
        RenderMultiLines renderResult = getCachedRenderResult(formatContext, parentResult, config);
        if (renderResult != null) {
            return renderResult;
        }
        int parentPosition = 0;
        if (parentResult != null) {
            if (config.getQueryConfig().isMajorKeywordsOnSeparateLine().booleanValue()
                    && !parentResult.isLastNonWhiteSpaceEqualToLinefeed()) {
                parentResult.addLine();
            }
            parentPosition = parentResult.getPosition();
        }
        int availableWidth = formatContext.getAvailableWidth();
        boolean doIndent = config.getQueryConfig().isIndent().booleanValue();
        FormatContext targetListContext = new FormatContext(config, formatContext)
                .setCommaSeparatedListGrouping(config.getTargetListGrouping());
        /*
         * First try to render it on the current line.
         */
        if (singleLineLength == 0) {
            singleLineLength = getSingleLineWidth(config);
        }
        if (singleLineLength > 0 && singleLineLength <= formatContext.getAvailableWidth()
                && singleLineLength <= config.getQueryConfig().getMaxSingleLineQuery().getValue()) {
            renderResult = new RenderMultiLines(this, formatContext, parentResult);
            int maxWidth = availableWidth;
            if (maxWidth > config.getQueryConfig().getMaxSingleLineQuery().getValue()) {
                maxWidth = config.getQueryConfig().getMaxSingleLineQuery().getValue();
            }
            formatContext.setAvailableWidth(maxWidth);
            for (ScanResult current = getStartScanResult(); current != null
                    && renderResult.getHeight() <= 1; current = current.getNext()) {
                if (current == targetList || current == distinctList) {
                    renderResult.addRenderResult(
                            current.beautify(targetListContext.setAvailableWidth(availableWidth - renderResult.getPosition()),
                                    renderResult, config),
                            formatContext);
                } else {
                    renderResult.addRenderResult(
                            current.beautify(formatContext.setAvailableWidth(availableWidth - renderResult.getPosition()),
                                    renderResult, config),
                            formatContext);
                }
            }
            if (renderResult.getHeight() == 1 && renderResult.getWidth() <= maxWidth) {
                formatContext.setAvailableWidth(availableWidth);
                return cacheRenderResult(renderResult, formatContext, parentResult);
            }
        }

        /*
         * Rendering on a single line didn't work out, so let's render multiline
         */
        renderResult = new RenderMultiLines(this, formatContext, parentResult).setIndentBase(parentPosition);
        if (doIndent) {
            renderResult.setIndent(config.getStandardIndent());
        } else {
            renderResult.setIndent(0);
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
                    renderResult.addRenderResult(current.beautify(formatContext, renderResult, config), formatContext);
                }
            } else {
                renderResult.addRenderResult(current.beautify(formatContext, renderResult, config), formatContext);
                current = current.getNext();
            }
            renderResult.addLine();
        }
        for (; current != null; current = current.getNext()) {
            if (current == targetList || current == distinctList) {
                renderResult.addRenderResult(
                        current.beautify(targetListContext.setAvailableWidth(availableWidth - renderResult.getPosition()),
                                renderResult, config),
                        formatContext);
            } else if (current instanceof WantsNewlineBefore) {
                renderResult.addLine();
                if (doIndent) {
                    renderResult.addRenderResult(current.beautify(
                            formatContext.setAvailableWidth(availableWidth - config.getStandardIndent()), renderResult,
                            config), formatContext);
                } else {
                    renderResult.addRenderResult(
                            current.beautify(formatContext.setAvailableWidth(availableWidth - renderResult.getPosition()),
                                    renderResult, config),
                            formatContext);
                }
            } else {
                renderResult.addRenderResult(
                        current.beautify(formatContext.setAvailableWidth(availableWidth - renderResult.getPosition()), renderResult,
                                config),
                        formatContext);
            }
        }

        formatContext.setAvailableWidth(availableWidth);
        return cacheRenderResult(renderResult, formatContext, parentResult);
    }

    /**
     * @see ScanResult#getSingleLineWidth(FormatConfiguration)
     */
    @Override
    public int getSingleLineWidth(FormatConfiguration config) {
        if (singleLineLength != 0) {
            return singleLineLength;
        }
        if (config.getQueryConfig().isMajorKeywordsOnSeparateLine().booleanValue()) {
            singleLineLength = -1;
            return singleLineLength;
        }
        /*
         * First check if this statement contains any keyword that would force it to be rendered multi-line anyway.
         */
        ScanResult node = getStartScanResult();
        if (config.getQueryConfig().isMajorKeywordsOnSeparateLine().booleanValue()) {
            node = getStartScanResult();
            for (node = node.getNext(); node != null; node = node.getNext()) {
                if (node instanceof IdentifierNode && !((IdentifierNode) node).isNotKeyword()
                        && ConfigUtil.isMajorKeywords(((IdentifierNode) node).getIdentifier())) {
                    singleLineLength = -1;
                    return singleLineLength;
                }
            }
        }
        int nodeLength;
        for (node = getStartScanResult(); node != null; node = node.getNext()) {
            if (node == targetList) {
                nodeLength = node.getSingleLineWidth(
                        new FormatConfiguration(config).setCommaSeparatedListGrouping(config.getTargetListGrouping()));
            } else {
                nodeLength = node.getSingleLineWidth(config);
            }
            if (nodeLength < 0) {
                singleLineLength = -1;
                return singleLineLength;
            }
            singleLineLength += nodeLength;
        }
        if (singleLineLength > config.getQueryConfig().getMaxSingleLineQuery().getValue()
                || singleLineLength > config.getLineWidth().getValue()) {
            singleLineLength = -1;
        }
        return singleLineLength;
    }

}
