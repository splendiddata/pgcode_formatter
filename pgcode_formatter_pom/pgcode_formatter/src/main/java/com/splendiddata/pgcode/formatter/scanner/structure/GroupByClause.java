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
 * The group by clause of a select statement
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class GroupByClause extends ClauseThatStartsWithMajorKeyword {
    /**
     * Constructor
     *
     * @param startNode
     *            The ScanResult that reads GROUP
     */
    public GroupByClause(ScanResult startNode) {
        super(ScanResultType.INTERPRETED, new IdentifierNode(startNode));
        assert "group".equalsIgnoreCase(startNode.toString());

        /*
         * by
         */
        ScanResult priorNode = getStartScanResult().locatePriorToNextInterpretable();
        ScanResult currentNode = priorNode.getNext();
        if (currentNode == null || !currentNode.is(ScanResultType.IDENTIFIER)
                || !"by".equalsIgnoreCase(currentNode.toString())) {
            priorNode.setNext(null);
            setNext(currentNode);
            return;
        }
        currentNode = new IdentifierNode(currentNode);
        priorNode.setNext(currentNode);

        /*
         * argument list
         */
        priorNode = currentNode.locatePriorToNextInterpretable();
        currentNode = CommaSeparatedList.withArbitraryEnd(priorNode.getNext(),
                node -> PostgresInputReader.interpretStatementBody(node), node -> {
                    if (!node.is(ScanResultType.IDENTIFIER)) {
                        return false;
                    }
                    switch (node.toString().toLowerCase()) {
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
        priorNode.setNext(currentNode);
        setNext(currentNode.getNext());
        currentNode.setNext(null);
    }

    /**
     * @see ClauseThatStartsWithMajorKeyword#getEndOfMajorKeyword()
     *
     * @return IdentifierNode the word BY from GROUP BY
     */
    @Override
    protected IdentifierNode getEndOfMajorKeyword() {
        return (IdentifierNode)getStartScanResult().getNextInterpretable();
    }
    
}
