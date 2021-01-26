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
 * A WHERE condition.
 */
public class WhereConditionNode extends ClauseThatStartsWithMajorKeyword {

    public WhereConditionNode(ScanResult scanResult) {
        super(ScanResultType.INTERPRETED, PostgresInputReader.toIdentifier(scanResult));
        ScanResult lastInterpreted = getStartScanResult();
        ScanResult previousNode = lastInterpreted;
        ScanResult currentNode = previousNode.getNext();
        mainLoop: for (previousNode = lastInterpreted.locatePriorToNextInterpretable();; previousNode = lastInterpreted
                .locatePriorToNextInterpretable()) {
            currentNode = previousNode.getNext();
            if (currentNode == null || (currentNode.isStatementEnd() || currentNode.is(ScanResultType.CLOSING_PARENTHESIS))) {
                break;
            }
            if (currentNode != null && currentNode.is(ScanResultType.IDENTIFIER)) {
                switch (currentNode.toString().toLowerCase()) {
                case "order":
                case "group":
                case "having":
                case "window":
                case "limit":
                case "offset":
                case "fetch":
                case "union":
                case "intersect":
                case "except":
                case "for":
                case "update":
                case "returning":
                case "on":
                    break mainLoop;
                default:
                    break;
                }
            }
            lastInterpreted = PostgresInputReader.interpretStatementBody(currentNode);
            previousNode.setNext(lastInterpreted);
        }
        setNext(lastInterpreted.getNext());
        lastInterpreted.setNext(null);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        ScanResult current = getStartScanResult();
        while (current != null && !current.isEof()) {
            result.append(current.toString());
            current = current.getNext();
        }
        return result.toString();
    }

}