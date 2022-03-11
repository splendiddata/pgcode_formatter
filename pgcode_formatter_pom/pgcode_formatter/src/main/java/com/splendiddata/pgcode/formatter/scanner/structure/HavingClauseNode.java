/*
 * Copyright (c) Splendid Data Product Development B.V. 2020 - 2021
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
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.internal.Util;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A having clause in a select statement.
 */
public class HavingClauseNode extends SrcNode implements WantsNewlineBefore {

    ScanResult havingCondition;

    public HavingClauseNode(ScanResult scanResult) {
        super(ScanResultType.HAVING_CLAUSE, scanResult);
        ScanResult previousNode = scanResult;
        ScanResult currentNode = scanResult;

        while (currentNode != null && !currentNode.isEof()) {

            if ("having".equalsIgnoreCase(currentNode.getText().toLowerCase())) {
                previousNode = previousNode.getNextInterpretable();

                currentNode = currentNode.getNextInterpretable();
                havingCondition = currentNode;
            }
            if (currentNode.isStatementEnd()) {
                previousNode.setNext(null);
                setNext(currentNode);

                return;
            }

            // WHERE, LIMIT, ORDER, ...
            switch (currentNode.getText().toLowerCase()) {
            case "where":
            case "order":
            case "window":
            case "limit":
            case "offset":
            case "fetch":
            case "for":
            case "union":
            case "loop":
                previousNode.setNext(null);
                setNext(currentNode);
                return;
            default:
                previousNode = currentNode;
                currentNode = currentNode.getNext();
                break;
            }
        }

        if (currentNode == null || currentNode.isEof()) {
            previousNode.setNext(null);
            setNext(currentNode);

            return;
        }
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

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult, FormatConfiguration config) {
        RenderMultiLines result = new RenderMultiLines(this, formatContext, parentResult);

        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            ScanResult current = Util.interpretStatement(node);

            RenderResult renderResult = current.beautify(formatContext, result, config);
            result.addRenderResult(renderResult, formatContext);
        }

        return result;
    }
}
