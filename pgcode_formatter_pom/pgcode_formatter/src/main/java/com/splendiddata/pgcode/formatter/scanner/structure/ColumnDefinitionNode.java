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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Definition of a column or a constraint in a create table statement
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class ColumnDefinitionNode extends SrcNode {
    private static final Logger log = LogManager.getLogger(ColumnDefinitionNode.class);

    private ScanResult name;
    private ScanResult dataType;
    private ScanResult columnConstraints;

    private ArgumentDefinitionOffsets argumentDefinitionOffsets;

    /**
     * Constructor
     *
     * @param startNode
     *            The first word of the argument. This can be the mode, the argument name or the data type
     */
    public ColumnDefinitionNode(ScanResult startNode) {
        super(ScanResultType.INTERPRETED, PostgresInputReader.interpretStatementBody(startNode));

        ScanResult lastNonWhitespace = getStartScanResult();
        ScanResult priorNode;
        ScanResult currentNode;
        
        name = lastNonWhitespace;

        priorNode = lastNonWhitespace.locatePriorToNextInterpretable();
        currentNode = priorNode.getNext();
        if (!(currentNode == null || currentNode.isStatementEnd() || currentNode.is(ScanResultType.CLOSING_PARENTHESIS)
                || (currentNode.is(ScanResultType.CHARACTER) && ",".equals(currentNode.toString())))) {
            lastNonWhitespace = PostgresInputReader.interpretStatementBody(currentNode);
            priorNode.setNext(lastNonWhitespace);
            dataType = lastNonWhitespace;
            for (priorNode = lastNonWhitespace.locatePriorToNextInterpretable();; priorNode = lastNonWhitespace
                    .locatePriorToNextInterpretable()) {
                currentNode = priorNode.getNext();
                if (currentNode == null || currentNode.isStatementEnd()
                        || currentNode.is(ScanResultType.CLOSING_PARENTHESIS)
                        || (currentNode.is(ScanResultType.CHARACTER) && ",".equals(currentNode.toString()))) {
                    break;
                }
                if (currentNode.is(ScanResultType.IDENTIFIER)) {
                    switch (currentNode.toString().toLowerCase()) {
                    case "constraint":
                    case "not":
                    case "null":
                    case "check":
                    case "default":
                    case "generated":
                    case "unique":
                    case "primary":
                    case "references":
                    case "like":
                        lastNonWhitespace = new ColumnConstraints(currentNode);
                        priorNode.setNext(lastNonWhitespace);
                        columnConstraints = lastNonWhitespace;
                        break;
                    default:
                        lastNonWhitespace = PostgresInputReader.interpretStatementBody(currentNode);
                        priorNode.setNext(lastNonWhitespace);
                        break;
                    }
                } else {
                    lastNonWhitespace = PostgresInputReader.interpretStatementBody(currentNode);
                    priorNode.setNext(lastNonWhitespace);
                }
            }
        }

        setNext(lastNonWhitespace.getNext());
        lastNonWhitespace.setNext(null);

        log.debug(() -> new StringBuilder().append("created: <").append(this).append(", name=").append(name)
                .append(", dataType=").append(dataType).append(", columnConstraints=").append(columnConstraints));
    }

    /**
     * @return String the name
     */
    public String getName() {
        return name == null ? "" : name.toString();
    }

    /**
     * @return String the dataType
     */
    public String getDataType() {
        if (dataType == null) {
            return "";
        }
        if (dataType.getNextInterpretable() == columnConstraints) {
            return dataType.toString();
        }
        // for example: timestamp(3) with time zone
        StringBuilder result = new StringBuilder().append(dataType);
        for (ScanResult node = dataType.getNextInterpretable(); node !=columnConstraints; node = node.getNextInterpretable()) {
            result.append(' ').append(node);
        }
        return result.toString();
    }

    /**
     * @return boolean true if column constraints exist
     */
    public boolean hasConstraints() {
        return columnConstraints != null;
    }

    /**
     * Renders this argument definition according to the specs
     *
     * @param formatContext
     *            The FormatContext to take into consideration
     * @param config
     *            The FormatConfiguration with rendering hints
     * @return RenderMultiLines The render result
     */
    public RenderMultiLines beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        if (argumentDefinitionOffsets == null) {
            argumentDefinitionOffsets = formatContext.getArgumentDefinitionOffsets();
        }
        log.debug(() -> new StringBuilder().append("beautify <").append(this).append(">, offsets=")
                .append(argumentDefinitionOffsets));
        RenderMultiLines result = new RenderMultiLines(this, formatContext);

        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            if (node == dataType) {
                if (argumentDefinitionOffsets != null && argumentDefinitionOffsets.getDataTypeOffset() != null) {
                    result.positionAt(argumentDefinitionOffsets.getDataTypeOffset().intValue());
                }
            } else if (node == columnConstraints) {
                if (argumentDefinitionOffsets != null
                        && argumentDefinitionOffsets.getDefaultExpressionOffset() != null) {
                    result.positionAt(argumentDefinitionOffsets.getDefaultExpressionOffset().intValue());
                }
            }
            result.addRenderResult(node.beautify(formatContext, parentResult, config), formatContext);
        }

        log.debug(() -> "render result = \n" + result.beautify());
        return result;
    }
}
