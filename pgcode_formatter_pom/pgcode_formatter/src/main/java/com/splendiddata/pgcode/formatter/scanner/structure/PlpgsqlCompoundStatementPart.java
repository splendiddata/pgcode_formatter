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

import java.util.function.Predicate;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * The compound part of a plpgsql compound statement.
 * 
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class PlpgsqlCompoundStatementPart extends SrcNode implements WantsNewlineBefore {

    /**
     * Constructor
     *
     * @param scanResult
     *            The word BEGIN or LOOP or THEN or ELSE ...
     */
    public PlpgsqlCompoundStatementPart(ScanResult scanResult) {
        this(scanResult, node -> false);
    }

    /**
     * Constructor
     * <p>
     * The CompoundStatementPart ends if
     * <ul>
     * <li>The beginEndLevel drops below the beginEndLevel
     * <li>when isBeyondTheBlock returns true
     * </ul>
     *
     * @param scanResult
     *            The start of a nested statement
     * @param isBeyondTheBlock
     *            A Predicate&lt;ScanResult&gt; that indicates that the tested node is outside this block
     */
    public PlpgsqlCompoundStatementPart(ScanResult scanResult, Predicate<ScanResult> isBeyondTheBlock) {
        super(ScanResultType.INTERPRETED, PostgresInputReader.interpretPlpgsqlStatementStart(scanResult));

        int beginEndLevel = scanResult.getBeginEndLevel();
        ScanResult lastInterpreted = getStartScanResult();
        ScanResult priorNode = lastInterpreted;
        ScanResult currentNode;
        for (currentNode = priorNode; currentNode != null && !currentNode.isEof()
                && currentNode.getBeginEndLevel() >= beginEndLevel
                && !isBeyondTheBlock.test(currentNode); currentNode = (priorNode == null ? null : priorNode.getNext())) {
            currentNode = PostgresInputReader.interpretPlpgsqlStatementStart(priorNode.getNext());
            if (currentNode != null && (!currentNode.is(ScanResultType.WHITESPACE) || currentNode.is(ScanResultType.LINEFEED))) {
                lastInterpreted = currentNode;
            }
            priorNode.setNext(currentNode);
            priorNode = currentNode;
        }

        setNext(lastInterpreted.getNext());
        lastInterpreted.setNext(null);
    }

    /**
     * @see ScanResult#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult, FormatConfiguration config) {
        RenderMultiLines result = new RenderMultiLines(this, formatContext).setIndent(0);
        for (ScanResult statement = getStartScanResult(); statement != null; statement = statement.getNext()) {
            if (statement.is(ScanResultType.LINEFEED)) {
                result.addLine();
            } else {
                if (statement.getType().isInterpretable()) {
                    result.addLine();
                }
                result.addRenderResult(statement.beautify(formatContext, result, config), formatContext);
            }
        }
        return result;
    }
}
