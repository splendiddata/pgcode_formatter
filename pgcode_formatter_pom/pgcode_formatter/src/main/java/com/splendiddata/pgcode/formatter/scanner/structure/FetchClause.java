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
 * The fetch clause of a select statement
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class FetchClause extends SrcNode implements WantsNewlineBefore {
    /**
     * Constructor
     *
     * @param startNode
     *            The word FETCH
     */
    public FetchClause(ScanResult startNode) {
        super(ScanResultType.INTERPRETED, new IdentifierNode(startNode));
        assert "fetch".equalsIgnoreCase(startNode.toString()) : "a Fetch Clause must start with the word FETCH, not: "
                + startNode;
        ScanResult lastInterpreted = getStartScanResult();
        ScanResult priorNode = lastInterpreted.locatePriorToNextInterpretable();
        ScanResult currentNode = priorNode.getNext();
        if (currentNode != null && currentNode.is(ScanResultType.IDENTIFIER)) {
            switch (currentNode.toString().toLowerCase()) {
            case "first":
            case "next":
                lastInterpreted = new IdentifierNode(currentNode);
                priorNode.setNext(lastInterpreted);
                priorNode = lastInterpreted.locatePriorToNextInterpretable();
                currentNode = priorNode.getNext();
                break;
            default:
                setNext(lastInterpreted.getNext());
                lastInterpreted.setNext(null);
                return;
            }
        }
        if (currentNode != null) {
            lastInterpreted = PostgresInputReader.interpretStatementBody(currentNode);
            priorNode.setNext(lastInterpreted);
            priorNode = lastInterpreted.locatePriorToNextInterpretable();
            currentNode = priorNode.getNext();
        }
        if (currentNode != null && currentNode.is(ScanResultType.IDENTIFIER)) {
            switch (currentNode.toString().toLowerCase()) {
            case "row":
            case "rows":
                lastInterpreted = new IdentifierNode(currentNode);
                priorNode.setNext(lastInterpreted);
                priorNode = lastInterpreted.locatePriorToNextInterpretable();
                currentNode = priorNode.getNext();
                break;
            default:
                setNext(lastInterpreted.getNext());
                lastInterpreted.setNext(null);
                return;
            }
        }
        if (currentNode != null && currentNode.is(ScanResultType.IDENTIFIER)
                && "only".equalsIgnoreCase(currentNode.toString())) {
            lastInterpreted = new IdentifierNode(currentNode);
            priorNode.setNext(lastInterpreted);
        }
        setNext(lastInterpreted.getNext());
        lastInterpreted.setNext(null);
    }
}
