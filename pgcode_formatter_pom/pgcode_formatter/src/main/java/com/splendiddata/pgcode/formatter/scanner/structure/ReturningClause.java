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
 * The RETURNING clause of an insert or update statement
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class ReturningClause extends SrcNode implements WantsNewlineBefore {

    /**
     * Constructor
     *
     * @param scanResult
     *            The word RETURNING
     */
    public ReturningClause(ScanResult scanResult) {
        super(ScanResultType.INTERPRETED, PostgresInputReader.toIdentifier(scanResult));
        assert "returning".equalsIgnoreCase(
                scanResult.toString()) : "A ReturningClause must start with the word RETURNING, not with: "
                        + scanResult;

        ScanResult previousNode = getStartScanResult().locatePriorToNextInterpretable();
        ScanResult currentNode = CommaSeparatedList.withArbitraryEnd(previousNode.getNext(),
                node -> PostgresInputReader.interpretStatementBody(node), node -> false);
        previousNode.setNext(currentNode);
        if (currentNode == null) {
            currentNode = previousNode;
        }
        setNext(currentNode.getNext());
        currentNode.setNext(null);
    }
}
