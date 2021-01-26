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
 * The "INTO" part in an INSERT statement, i.e. INTO table_name.
 */
public class IntoClauseNode extends ClauseThatStartsWithMajorKeyword {

    /**
     * Constructor.
     * 
     * @param scanResult
     *            The node that contains INTO as part of INSERT statement.
     */
    public IntoClauseNode(ScanResult scanResult) {
        super(ScanResultType.INTO_CLAUSE, new IdentifierNode(scanResult));
        assert "into".equalsIgnoreCase(scanResult.getText()) : "An into clause must start with the word into";

        ScanResult prev = this.getStartScanResult();
        ScanResult next;
        for (next = PostgresInputReader.interpretStatementBody(prev.getNext()); next != null
                && !next.getType().isInterpretable(); next = PostgresInputReader
                        .interpretStatementBody(prev.getNext())) {
            prev.setNext(next);
            prev = next;
        }
        if (next.is(ScanResultType.IDENTIFIER)) {
            if ("strict".equalsIgnoreCase(next.getText())) {
                prev.setNext(next);
                prev = next;
                for (next = PostgresInputReader.interpretStatementBody(prev.getNext()); next != null
                        && !next.getType().isInterpretable(); next = PostgresInputReader
                                .interpretStatementBody(prev.getNext())) {
                    prev.setNext(next);
                    prev = next;
                }
            } else {
                switch (next.getText().toLowerCase()) {
                case "temporary":
                case "temp":
                case "unlogged":
                    prev.setNext(next);
                    prev = next;
                    for (next = PostgresInputReader.interpretStatementBody(prev.getNext()); next != null
                            && !next.getType().isInterpretable(); next = PostgresInputReader
                                    .interpretStatementBody(prev.getNext())) {
                        prev.setNext(next);
                        prev = next;
                    }
                    break;
                default:
                    break;
                }
                if (next.is(ScanResultType.IDENTIFIER) && "table".equalsIgnoreCase(next.getText())) {
                    prev.setNext(next);
                    prev = next;
                    for (next = PostgresInputReader.interpretStatementBody(prev.getNext()); next != null
                            && !next.getType().isInterpretable(); next = PostgresInputReader
                                    .interpretStatementBody(prev.getNext())) {
                        prev.setNext(next);
                        prev = next;
                    }
                }
            }
        }
        if (next.is(ScanResultType.IDENTIFIER)) {
            prev.setNext(next);
            ((IdentifierNode) next).setNotKeyword(true);
            prev = next;
            next = prev.getNext();
        }
        prev.setNext(null);
        setNext(next);
    }

    /**
     * @see ClauseThatStartsWithMajorKeyword#getEndOfMajorKeyword()
     *
     * @return IdentifierNode either INTO or STRICT
     */
    @Override
    protected IdentifierNode getEndOfMajorKeyword() {
        ScanResult strict = getStartScanResult().getNextInterpretable();
        if (strict != null && strict.is(ScanResultType.IDENTIFIER) && "strict".equalsIgnoreCase(strict.toString())) {
            return (IdentifierNode) strict;
        }
        return (IdentifierNode) getStartScanResult();
    }
}