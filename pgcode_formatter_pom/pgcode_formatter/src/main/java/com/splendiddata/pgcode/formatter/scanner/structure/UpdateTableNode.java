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

import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * An update statement or the update part of an on conflict node in an insert statement
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class UpdateTableNode extends SrcNode {

    /**
     * Constructor
     *
     * @param scanResult
     *            The word UPDATE
     * @param isStatement
     *            Boolean to indicate if this is an update statement (true) or an update clause (false).
     *            <p>
     *            If it is a statement, then an ending semi-colon is part of the statement. In an update clause the
     *            ending semi-colon will be part of the statement that contains this clause.
     */
    public UpdateTableNode(ScanResult scanResult, boolean isStatement) {
        this(scanResult, isStatement, node -> false);
    }

    /**
     * Constructor
     *
     * @param scanResult
     *            The word UPDATE
     * @param isStatement
     *            Boolean to indicate if this is an update statement (true) or an update clause (false).
     *            <p>
     *            If it is a statement, then an ending semi-colon is part of the statement. In an update clause the
     *            ending semi-colon will be part of the statement that contains this clause.
     * @param isComplete
     *            A Predicate&lt;ScanResult&gt; that indicates that the list for ScanResults has reached a node beyond
     *            the update table statement.
     */
    public UpdateTableNode(ScanResult scanResult, boolean isStatement, Predicate<ScanResult> isComplete) {
        super(ScanResultType.INTERPRETED, PostgresInputReader.toIdentifier(scanResult));
        assert "update".equalsIgnoreCase(
                scanResult.toString()) : "An UpdateTableNode must start with the word UPDATE, not with: "
                        + scanResult.toString();

        ScanResult lastInterpreted = getStartScanResult();
        ScanResult priorNode = lastInterpreted.locatePriorToNextInterpretable();
        ScanResult currentNode = priorNode.getNext();

        for (priorNode = lastInterpreted.locatePriorToNextInterpretable();; priorNode = currentNode
                .locatePriorToNextInterpretable()) {
            currentNode = priorNode.getNext();
            if (currentNode == null || currentNode.isStatementEnd()) {
                break;
            }
            if (currentNode.is(ScanResultType.IDENTIFIER)) {
                switch (currentNode.toString().toLowerCase()) {
                case "set":
                    currentNode = new SetClauseNode(currentNode);
                    break;
                case "from":
                    currentNode = new FromClause(currentNode);
                    break;
                case "where":
                    currentNode = new WhereConditionNode(currentNode);
                    break;
                case "returning":
                    currentNode = new ReturningClause(currentNode);
                    break;
                default:
                    currentNode = PostgresInputReader.interpretStatementBody(currentNode);
                    break;
                }
            } else {
                if (isComplete.test(currentNode)) {
                    setNext(lastInterpreted.getNext());
                    lastInterpreted.setNext(null);
                    return;
                }
                currentNode = PostgresInputReader.interpretStatementBody(currentNode);
            }
            lastInterpreted = currentNode;
            priorNode.setNext(currentNode);
        }

        if (currentNode != null && currentNode.is(ScanResultType.SEMI_COLON) && isStatement) {
            lastInterpreted = currentNode;
        }
        setNext(lastInterpreted.getNext());
        lastInterpreted.setNext(null);
    }
}
