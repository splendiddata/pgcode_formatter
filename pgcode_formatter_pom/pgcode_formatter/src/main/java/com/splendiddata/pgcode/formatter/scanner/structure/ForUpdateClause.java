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
 * A FOR clause in a select statement
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class ForUpdateClause extends SrcNode implements WantsNewlineBefore {
    /**
     * Constructor
     *
     * @param startNode
     *            The word FOR that starts the for update clause
     */
    public ForUpdateClause(ScanResult startNode) {
        super(ScanResultType.INTERPRETED, PostgresInputReader.toIdentifier(startNode));
        ScanResult lastInterpreted = getStartScanResult();
        ScanResult priorNode = lastInterpreted.locatePriorToNextInterpretable();
        ScanResult currentNode = priorNode.getNext();
        if (currentNode == null || !currentNode.is(ScanResultType.IDENTIFIER)) {
            setNext(lastInterpreted.getNext());
            lastInterpreted.setNext(null);
            return;
        }
        switch (currentNode.toString().toLowerCase()) {
        case "no":
            lastInterpreted = PostgresInputReader.toIdentifier(currentNode);
            priorNode.setNext(lastInterpreted);
            priorNode = lastInterpreted.locatePriorToNextInterpretable();
            currentNode = priorNode.getNext();
            //$FALL-THROUGH$
        case "key":
            lastInterpreted = PostgresInputReader.toIdentifier(currentNode);
            priorNode.setNext(lastInterpreted);
            priorNode = lastInterpreted.locatePriorToNextInterpretable();
            currentNode = priorNode.getNext();
            //$FALL-THROUGH$
        case "update":
        case "share":
            lastInterpreted = PostgresInputReader.toIdentifier(currentNode);
            priorNode.setNext(lastInterpreted);
            priorNode = lastInterpreted.locatePriorToNextInterpretable();
            currentNode = priorNode.getNext();
            break;
        default:
            setNext(lastInterpreted.getNext());
            lastInterpreted.setNext(null);
            return;
        }

        if (currentNode == null || !currentNode.is(ScanResultType.IDENTIFIER)) {
            setNext(lastInterpreted.getNext());
            lastInterpreted.setNext(null);
            return;
        }
        if ("of".equalsIgnoreCase(currentNode.toString())) {
            lastInterpreted = PostgresInputReader.toIdentifier(currentNode);
            priorNode.setNext(lastInterpreted);
            priorNode = lastInterpreted.locatePriorToNextInterpretable();
            currentNode = priorNode.getNext();
            if (currentNode != null) {
                lastInterpreted = CommaSeparatedList.withArbitraryEnd(currentNode,
                        node -> PostgresInputReader.interpretStatementBody(node), node -> {
                            if (!node.is(ScanResultType.IDENTIFIER)) {
                                return false;
                            }
                            switch (node.toString().toLowerCase()) {
                            case "nowait":
                            case "skip":
                            case "for":
                            case "into":
                                return true;
                            default:
                                return false;
                            }
                        });
                priorNode.setNext(lastInterpreted);
                priorNode = lastInterpreted.locatePriorToNextInterpretable();
                currentNode = priorNode.getNext();
            }
        }
        if (currentNode == null || !currentNode.is(ScanResultType.IDENTIFIER)) {
            setNext(lastInterpreted.getNext());
            lastInterpreted.setNext(null);
            return;
        }
        if ("nowait".equalsIgnoreCase(currentNode.toString())) {
            lastInterpreted = PostgresInputReader.toIdentifier(currentNode);
            priorNode.setNext(lastInterpreted);

        } else if ("skip".equalsIgnoreCase(currentNode.toString())) {
            lastInterpreted = PostgresInputReader.toIdentifier(currentNode);
            priorNode.setNext(lastInterpreted);
            priorNode = lastInterpreted.locatePriorToNextInterpretable();
            currentNode = priorNode.getNext();
            if (currentNode == null || !currentNode.is(ScanResultType.IDENTIFIER)) {
                setNext(lastInterpreted.getNext());
                lastInterpreted.setNext(null);
                return;
            }
            if ("locked".equalsIgnoreCase(currentNode.toString())) {
                lastInterpreted = PostgresInputReader.toIdentifier(currentNode);
                priorNode.setNext(lastInterpreted);
            }
        }

        setNext(lastInterpreted.getNext());
        lastInterpreted.setNext(null);
    }
}
