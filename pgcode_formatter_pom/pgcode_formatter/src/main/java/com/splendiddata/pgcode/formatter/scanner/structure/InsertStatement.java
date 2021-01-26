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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Implementation of the INSERT statement
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class InsertStatement extends SrcNode implements WantsNewlineBefore {
    private static final Logger log = LogManager.getLogger(InsertStatement.class);

    /**
     * Constructor
     *
     * @param startNode
     *            The word INSERT
     */
    public InsertStatement(ScanResult startNode) {
        super(ScanResultType.INTERPRETED, PostgresInputReader.toIdentifier(startNode));
        assert "insert".equalsIgnoreCase(getStartScanResult().getText()
                .toLowerCase()) : "An InsertStatement must start with the word INSERT, not with "
                        + getStartScanResult().getText();

        ScanResult lastInterpreted = getStartScanResult();
        ScanResult priorNode = lastInterpreted.locatePriorToNextInterpretable();
        ScanResult currentNode = priorNode.getNext();
        /*
         * An insert statement may be wrapped in parentheses, for example in a with clause
         */
        int parenthesesLevel = lastInterpreted.getParenthesisLevel();

        /*
         * The rest of the statement
         */
        for (priorNode = lastInterpreted.locatePriorToNextInterpretable(); priorNode
                .getNext() != null; priorNode = lastInterpreted.locatePriorToNextInterpretable()) {
            currentNode = priorNode.getNext();
            if (currentNode.isStatementEnd() || currentNode.getParenthesisLevel() < parenthesesLevel) {
                break;
            }
            if (currentNode.is(ScanResultType.IDENTIFIER)) {
                switch (currentNode.toString().toLowerCase()) {
                case "into":
                    lastInterpreted = new IntoClauseNode(currentNode);
                    if (log.isTraceEnabled()) {
                        log.trace("into = <" + lastInterpreted + ">");
                    }
                    break;
                case "values":
                case "select":
                    lastInterpreted = new SelectStatement(currentNode,
                            node -> node == null || node.isStatementEnd()
                                    || (node.is(ScanResultType.IDENTIFIER) && "on".equalsIgnoreCase(node.toString())
                                            && node.getNextInterpretable() != null
                                            && node.getNextInterpretable().is(ScanResultType.IDENTIFIER)
                                            && "conflict".equalsIgnoreCase(node.getNextInterpretable().toString())));
                    if (log.isTraceEnabled()) {
                        log.trace("into = <" + lastInterpreted + ">");
                    }
                    break;
                case "on":
                    lastInterpreted = new OnConflictNode(currentNode);
                    if (log.isTraceEnabled()) {
                        log.trace("on conflict = <" + lastInterpreted + ">");
                    }
                    break;
                case "returning":
                    lastInterpreted = new ReturningClause(currentNode);
                    if (log.isTraceEnabled()) {
                        log.trace("returning = <" + lastInterpreted + ">");
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

        if (currentNode != null && currentNode.is(ScanResultType.SEMI_COLON)) {
            lastInterpreted = new SemiColonNode(currentNode);
            priorNode.setNext(lastInterpreted);
        }
        setNext(lastInterpreted.getNext());
        lastInterpreted.setNext(null);
        log.debug(() -> "constructed insert statement = " + this);
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderMultiLines beautify(FormatContext formatContext, RenderMultiLines parentResult, FormatConfiguration config) {
        if (!config.getQueryConfig().isMajorKeywordsOnSeparateLine().booleanValue()) {
            RenderMultiLines result = new RenderMultiLines(this, formatContext);
            for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
                result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
            }
            if (result.getHeight() == 1
                    && result.getPosition() <= config.getQueryConfig().getMaxSingleLineQuery().getValue()) {
                return result;
            }
        }

        String indent = config.getQueryConfig().isIndent().booleanValue() ? FormatContext.indent(true) : "";
        RenderMultiLines result = new RenderMultiLines(this, formatContext);
        FormatContext contentContext = new FormatContext(config, formatContext)
                .setAvailableWidth(formatContext.getAvailableWidth() - indent.length());
        ScanResult node = getStartScanResult();
        result.addRenderResult(node.beautify(contentContext, result, config), formatContext);
        for (node = node.getNext(); node != null; node = node.getNext()) {
            RenderResult contentResult = node.beautify(contentContext, result, config);
            if (node instanceof WantsNewlineBefore && !(node instanceof IntoClauseNode)) {
                result.addLine();
            }
            result.addRenderResult(contentResult, formatContext);
        }
        return result;
    }

}
