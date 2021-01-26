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

import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Part of a WITH statement. See with_query in <a href="https://www.postgresql.org/docs/current/sql-select.html">https://www.postgresql.org/docs/current/sql-select.html</a>
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class WithQuery extends SrcNode {

    /**
     * Constructor
     *
     * @param startNode
     *            The name of the with query
     */
    public WithQuery(ScanResult startNode) {
        super(ScanResultType.INTERPRETED, PostgresInputReader.toIdentifier(startNode));
        ScanResult priorNode = getStartScanResult().locatePriorToNextInterpretable();
        ScanResult currentNode = priorNode.getNext();
        if (currentNode != null && currentNode.is(ScanResultType.OPENING_PARENTHESIS)) {
            currentNode = new InParentheses(currentNode, node -> CommaSeparatedList.withArbitraryEnd(node,
                    innerNode -> PostgresInputReader.interpretStatementBody(innerNode), innerNode -> false));
            priorNode.setNext(currentNode);
            priorNode = currentNode.locatePriorToNextInterpretable();
            currentNode = priorNode.getNext();
        }
        if (currentNode == null || !"as".equalsIgnoreCase(currentNode.toString())) {
            if (currentNode != null) {
                setNext(currentNode.getNext());
                currentNode.setNext(null);
            }
            return;
        }
        currentNode = PostgresInputReader.toIdentifier(currentNode);
        priorNode.setNext(currentNode);
        priorNode = currentNode.locatePriorToNextInterpretable();
        currentNode = priorNode.getNext();
        if (currentNode != null && "not".equalsIgnoreCase(currentNode.toString())) {
            currentNode = PostgresInputReader.toIdentifier(currentNode);
            priorNode.setNext(currentNode);
            priorNode = currentNode.locatePriorToNextInterpretable();
            currentNode = priorNode.getNext();
        }
        if (currentNode != null && "materialized".equalsIgnoreCase(currentNode.toString())) {
            currentNode = PostgresInputReader.toIdentifier(currentNode);
            priorNode.setNext(currentNode);
            priorNode = currentNode.locatePriorToNextInterpretable();
            currentNode = priorNode.getNext();
        }
        if (currentNode != null && currentNode.is(ScanResultType.OPENING_PARENTHESIS)) {
            currentNode = new InParentheses(currentNode,
                    node -> PostgresInputReader.interpretStatementStart(node, scanResultNode -> {
                        if (scanResultNode.is(ScanResultType.CLOSING_PARENTHESIS)) {
                            return true;
                        }
                        return false;
                    }));
            priorNode.setNext(currentNode);
        }
        setNext(currentNode.getNext());
        currentNode.setNext(null);
    }

    /**
     * @see com.splendiddata.pgcode.formatter.scanner.structure.SrcNode#getText()
     *
     * @return The text of this WITH clause
     */
    @Override
    public String getText() {
        return toString();
    }
}
