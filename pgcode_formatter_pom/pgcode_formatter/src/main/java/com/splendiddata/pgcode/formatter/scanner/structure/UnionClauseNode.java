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

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Implements a union or intersect or except clause in a select statement
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class UnionClauseNode extends SrcNode implements WantsNewlineBefore {

    ScanResult constituentParts;

    /**
     * Constructor
     *
     * @param scanResult
     *            The word 'union', 'intersect' or 'except' that starts the clause
     */
    public UnionClauseNode(ScanResult scanResult) {
        super(ScanResultType.UNION_CLAUSE, scanResult);
        ScanResult currentNode = scanResult;
        assert "union".equalsIgnoreCase(currentNode.toString()) || "intersect".equalsIgnoreCase(currentNode.toString())
                || "except".equalsIgnoreCase(
                        currentNode.toString()) : "Expecting 'UNION' or 'INTERSECT' or 'EXCEPT' but got: "
                                + currentNode;
        constituentParts = new IdentifierNode(currentNode);
        ScanResult previousNode = constituentParts;
        for (currentNode = previousNode
                .getNext(); currentNode != null
                        && !currentNode.isStatementEnd()
                        && !currentNode.is(ScanResultType.OPENING_PARENTHESIS)
                        && !("select".equalsIgnoreCase(currentNode.toString())
                                || "values".equalsIgnoreCase(currentNode.toString())); currentNode = previousNode
                                        .getNext()) {
            previousNode.setNext(PostgresInputReader.interpretStatementBody(currentNode));
            previousNode = previousNode.getNext();
        }

        if (currentNode != null && !currentNode.isStatementEnd() && ("select".equalsIgnoreCase(currentNode.toString()) || currentNode.is(ScanResultType.OPENING_PARENTHESIS)
                || "values".equalsIgnoreCase(currentNode.toString()))) {
            if (currentNode.is(ScanResultType.OPENING_PARENTHESIS)) {
                InParentheses next = new InParentheses(currentNode, argStart -> CommaSeparatedList.withArbitraryEnd(argStart,
                        argNode -> PostgresInputReader.interpretStatementBody(argNode), argNode -> false));
                previousNode.setNext(next);

            } else {
                SelectStatement next = new SelectStatement(currentNode, node -> {
                    if (node.is(ScanResultType.SEMI_COLON)) {
                        return true;
                    }
                    return false;
                });

                previousNode.setNext(next);
            }
            currentNode = previousNode.getNext();
        }
        setNext(currentNode.getNext());
        currentNode.setNext(null);
    }

    /**
     * @see ScanResult#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        RenderMultiLines result = new RenderMultiLines(this, formatContext).setIndent(0);
        for (ScanResult node = constituentParts; node != null; node = node.getNext()) {
            if (ScanResultType.SELECT_STATEMENT.equals(node.getType())) {
                result.addLine();
            }
            RenderResult renderResult = node.beautify(formatContext, result, config);
            result.addRenderResult(renderResult, formatContext);
        }

        return result;
    }

}
