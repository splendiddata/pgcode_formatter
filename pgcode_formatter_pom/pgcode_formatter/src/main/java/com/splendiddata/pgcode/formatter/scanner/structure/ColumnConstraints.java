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
import com.splendiddata.pgcode.formatter.internal.Util;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A set of column constraints
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class ColumnConstraints extends SrcNode {
    private RenderMultiLines renderedSimpleResult = null;

    /**
     * Constructor
     *
     * @param start
     *            The first ScanResult in this constraints element
     */
    public ColumnConstraints(ScanResult start) {
        super(ScanResultType.INTERPRETED, PostgresInputReader.interpretStatementBody(start));
        ScanResult lastNonWhitespace = getStartScanResult();
        ScanResult priorNode;
        ScanResult currentNode = null;
        for (priorNode = lastNonWhitespace;; priorNode = currentNode) {
            currentNode = priorNode.getNext();
            if (currentNode == null || currentNode.isStatementEnd()
                    || currentNode.is(ScanResultType.CLOSING_PARENTHESIS)
                    || (currentNode.is(ScanResultType.CHARACTER) && ",".equals(currentNode.toString()))) {
                break;
            }
            if (!(currentNode.is(ScanResultType.WHITESPACE) || currentNode.is(ScanResultType.LINEFEED))) {
                currentNode = PostgresInputReader.interpretStatementBody(currentNode);
                priorNode.setNext(currentNode);
                lastNonWhitespace = currentNode;
            }
        }
        setNext(lastNonWhitespace.getNext());
        lastNonWhitespace.setNext(null);
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderMultiLines beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        if (renderedSimpleResult != null) {
            /*
             * Return a cached render result
             */
            return renderedSimpleResult;
        }
        RenderMultiLines result = Util.renderStraightForward(getStartScanResult(),
                new RenderMultiLines(this, formatContext).setIndent(0), formatContext, config);
        if (result.getHeight() <= 1) {
            /*
             * If the render result fits on a single line, then the result will not change if rendered again. So in that
             * case we might as well cache it.
             */
            renderedSimpleResult = result;
        }
        return result;
    }
}
