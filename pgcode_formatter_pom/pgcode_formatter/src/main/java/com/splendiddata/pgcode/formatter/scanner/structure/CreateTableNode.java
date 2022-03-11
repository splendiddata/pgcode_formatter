/*
 * Copyright (c) Splendid Data Product Development B.V. 2020 - 2022
 *
 * This program is free software: You may redistribute and/or modify under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or (at Client's option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, Client should
 * obtain one via www.gnu.org/licenses/.
 */

package com.splendiddata.pgcode.formatter.scanner.structure;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.TableDefinitionType;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Create table class. There are different ways to create a table in postgres. Examples:
 * <ol>
 * <li>CREATE TABLE circles (
 * <p>
 * c circle,
 * <p>
 * EXCLUDE USING gist (c WITH &amp;&amp;)
 * <p>
 * );
 * <li>CREATE TABLE employees OF employee_type ...
 * <li>CREATE TABLE measurement_y2016m07 PARTITION OF measurement ...
 * </ol>
 *
 */
public class CreateTableNode extends SrcNode {
    private static final Logger log = LogManager.getLogger(CreateTableNode.class);

    private InParentheses columnsAndConstraints;

    private int singleLineWidth;

    /**
     * Constructor.
     * 
     * @param startNode
     *            The start node of create table statement, i.e. The node that contains "create".
     */
    public CreateTableNode(ScanResult startNode) {
        super(ScanResultType.FUNCTION_DEFINITION, PostgresInputReader.toIdentifier(startNode));

        ScanResult lastInterpreted = getStartScanResult();
        ScanResult priorNode;
        ScanResult currentNode = null;

        /*
         * Accept everything until the table name
         */
        for (priorNode = lastInterpreted.locatePriorToNextInterpretable();; priorNode = lastInterpreted
                .locatePriorToNextInterpretable()) {
            currentNode = priorNode.getNext();
            if (currentNode == null || currentNode.isStatementEnd() || !currentNode.is(ScanResultType.IDENTIFIER)) {
                break;
            }
            switch (currentNode.toString().toLowerCase()) {
            case "global":
            case "local":
            case "temporary":
            case "temp":
            case "unlogged":
            case "table":
            case "if":
            case "not":
            case "exists":
                lastInterpreted = PostgresInputReader.toIdentifier(currentNode);
                priorNode.setNext(lastInterpreted);
                continue;
            default:
                break;
            }
            break;
        }

        /*
         * table name
         */
        if (currentNode != null && currentNode.is(ScanResultType.IDENTIFIER)) {
            lastInterpreted = PostgresInputReader.toIdentifier(currentNode);
            priorNode.setNext(lastInterpreted);
            priorNode = lastInterpreted.locatePriorToNextInterpretable();
            currentNode = priorNode.getNext();
            if (currentNode != null && currentNode.is(ScanResultType.IDENTIFIER)
                    && "partition".equalsIgnoreCase(currentNode.toString())) {
                lastInterpreted = PostgresInputReader.toIdentifier(currentNode);
                priorNode.setNext(lastInterpreted);
                priorNode = lastInterpreted.locatePriorToNextInterpretable();
                currentNode = priorNode.getNext();
            }
            if (currentNode != null && currentNode.is(ScanResultType.IDENTIFIER)
                    && "of".equalsIgnoreCase(currentNode.toString())) {
                lastInterpreted = PostgresInputReader.toIdentifier(currentNode);
                priorNode.setNext(lastInterpreted);
                priorNode = lastInterpreted.locatePriorToNextInterpretable();
                currentNode = priorNode.getNext();
                if (currentNode != null && currentNode.is(ScanResultType.IDENTIFIER)) {
                    // type name or parent table name
                    lastInterpreted = PostgresInputReader.toIdentifier(currentNode);
                    priorNode.setNext(lastInterpreted);
                    priorNode = lastInterpreted.locatePriorToNextInterpretable();
                    currentNode = priorNode.getNext();
                }
            }
        }

        /*
         * column list (if any)
         */
        if (currentNode != null && currentNode.is(ScanResultType.OPENING_PARENTHESIS)) {
            columnsAndConstraints = new InParentheses(currentNode,
                    aNode -> CommaSeparatedList.ofDistinctElementTypes(aNode, elementNode -> {
                        ScanResult firstEffectiveNode = elementNode;
                        if (!firstEffectiveNode.getType().isInterpretable()) {
                            firstEffectiveNode = firstEffectiveNode.getNextInterpretable();
                        }
                        if (firstEffectiveNode.is(ScanResultType.IDENTIFIER)) {
                            switch (firstEffectiveNode.toString().toLowerCase()) {
                            case "constraint":
                            case "check":
                            case "unique":
                            case "primary":
                            case "exclude":
                            case "foreign":
                            case "like":
                                return new TableConstraint(elementNode);
                            default:
                                return new ColumnDefinitionNode(elementNode);
                            }
                        }
                        return new TableConstraint(elementNode);
                    }));
            lastInterpreted = columnsAndConstraints;
            priorNode.setNext(lastInterpreted);
        }

        for (priorNode = lastInterpreted.locatePriorToNextInterpretable();; priorNode = lastInterpreted
                .locatePriorToNextInterpretable()) {
            currentNode = priorNode.getNext();
            if (currentNode == null || currentNode.isStatementEnd()) {
                break;
            }
            lastInterpreted = PostgresInputReader.interpretStatementBody(currentNode);
            priorNode.setNext(lastInterpreted);
        }

        if (currentNode != null && currentNode.is(ScanResultType.SEMI_COLON)) {
            lastInterpreted = new SemiColonNode(currentNode);
            priorNode.setNext(lastInterpreted);
        }

        setNext(lastInterpreted.getNext());
        lastInterpreted.setNext(null);
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderMultiLines beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        RenderMultiLines renderResult = getCachedRenderResult(formatContext, parentResult, config);
        if (renderResult != null) {
            return renderResult;
        }
        int parentPosition = parentResult == null ? 0 : parentResult.getPosition();

        /*
         * how do we format the column definitions and constraints?
         */
        TableDefinitionType argumentListConfig = config.getTableDefinition();
        ArgumentDefinitionOffsets argumentDefinitionOffsets = getArgumentDefinitionOffsets(argumentListConfig);
        FormatContext context = new FormatContext(config, formatContext)
                .setArgumentDefinitionOffsets(argumentDefinitionOffsets)
                .setCommaSeparatedListGrouping(config.getTableDefinition().getArgumentGrouping());
        renderResult = new RenderMultiLines(this, context, parentResult).setIndentBase(parentPosition)
                .setIndent(config.getStandardIndent());

        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            if (node == columnsAndConstraints) {
                renderResult.addRenderResult(node.beautify(context, renderResult, config), context);
            } else if (node.is(ScanResultType.IDENTIFIER)) {
                switch (node.toString().toLowerCase()) {
                case "inherits":
                case "partition":
                case "using":
                case "with":
                case "on":
                case "tablespace":
                    renderResult.addLine();
                    break;
                default:
                    break;
                }
                renderResult.addRenderResult(node.beautify(context, renderResult, config), context);
            } else {
                renderResult.addRenderResult(node.beautify(context, renderResult, config), context);
            }
        }
        return cacheRenderResult(renderResult, formatContext, parentResult);
    }

    /**
     * Determines the offsets within the constituent parts of column definition
     *
     * @param config
     *            The TableDefinitionType that describes how to format a column description
     * @return ArgumentDefinitionOffsets for rendering
     */
    private ArgumentDefinitionOffsets getArgumentDefinitionOffsets(TableDefinitionType config) {
        ArgumentDefinitionOffsets result = new ArgumentDefinitionOffsets();

        if (columnsAndConstraints == null) {
            return result;
        }

        CommaSeparatedList list = (CommaSeparatedList) columnsAndConstraints.getStartScanResult()
                .getNextInterpretable();
        /*
         * Determine the maximum lengths of the constituent parts of the arguments
         */
        int maxNameLength = 0;
        int maxDataTypeLength = 0;
        ScanResult node;
        for (ListElement element : list.getElements()) {
            node = element.getStartScanResult();
            if (!node.getType().isInterpretable()) {
                node = node.getNextInterpretable();
            }
            if (node instanceof ColumnDefinitionNode) {
                ColumnDefinitionNode argument = (ColumnDefinitionNode) node;
                int length = argument.getName().length();
                if (length > maxNameLength) {
                    maxNameLength = length;
                }
                length = argument.getDataType().length();
                if (length > maxDataTypeLength) {
                    maxDataTypeLength = length;
                }
            }
        }

        /*
         * Position of the data type
         */
        int offset = maxNameLength + 1;
        if (maxNameLength > 0) {
            offset += maxNameLength + 1;
        }
        switch (config.getDataType().getAlignment()) {
        case AT_HORIZONTAL_POSITION:
            offset = config.getDataType().getMinPosition().intValue();
            result.setDataTypeOffset(config.getDataType().getMinPosition());
            break;
        case SUBSEQUENT:
            break;
        case VERTICALLY_ALIGNED:
            if (offset <= config.getDataType().getMinPosition().intValue()) {
                offset = config.getDataType().getMinPosition().intValue();
                result.setDataTypeOffset(config.getDataType().getMinPosition());
            } else if (offset >= config.getDataType().getMaxPosition().intValue()) {
                offset = config.getDataType().getMaxPosition().intValue();
                result.setDataTypeOffset(config.getDataType().getMaxPosition());
            } else {
                result.setDataTypeOffset(Integer.valueOf(offset));
            }
            break;
        default:
            assert false : "Unknown " + config.getDataType().getAlignment().getClass().getName() + " value: "
                    + config.getDataType().getAlignment();
            break;
        }

        /*
         * Position of the column constraint
         */
        offset += maxDataTypeLength + 1;
        switch (config.getColumnContraint().getAlignment()) {
        case AT_HORIZONTAL_POSITION:
            offset = config.getColumnContraint().getMinPosition().intValue();
            result.setDefaultExpressionOffset(config.getColumnContraint().getMinPosition());
            break;
        case SUBSEQUENT:
            break;
        case VERTICALLY_ALIGNED:
            if (offset <= config.getColumnContraint().getMinPosition().intValue()) {
                offset = config.getColumnContraint().getMinPosition().intValue();
                result.setDefaultExpressionOffset(config.getColumnContraint().getMinPosition());
            } else if (offset >= config.getColumnContraint().getMaxPosition().intValue()) {
                offset = config.getColumnContraint().getMaxPosition().intValue();
                result.setDefaultExpressionOffset(config.getColumnContraint().getMaxPosition());
            } else {
                result.setDefaultExpressionOffset(Integer.valueOf(offset));
            }
            break;
        case UNDER_DATA_TYPE:
            result.setDefaultExpressionOffset(result.getDataTypeOffset());
            break;
        default:
            assert false : "Unknown " + config.getColumnContraint().getAlignment().getClass().getName() + " value: "
                    + config.getColumnContraint().getAlignment();
            break;
        }

        log.debug(() -> "getArgumentDefinitionOffsets() = " + result);
        return result;
    }

    /**
     * @see com.splendiddata.pgcode.formatter.scanner.ScanResult#getSingleLineWidth(com.splendiddata.pgcode.formatter.FormatConfiguration)
     *
     * @param config
     * @return
     */
    @Override
    public int getSingleLineWidth(FormatConfiguration config) {
        if (singleLineWidth == 0) {
            for (ScanResult node = columnsAndConstraints.getStartScanResult(); node != null; node = node
                    .getNextInterpretable()) {
                if (node instanceof CommaSeparatedList) {
                    if (((CommaSeparatedList) node).getElements().size() > 1) {
                        singleLineWidth = -1;
                        return singleLineWidth;
                    }
                    break;
                }
            }
        }
        int elementWidth;
        for (ScanResult node = columnsAndConstraints.getStartScanResult(); node != null; node = node
                .getNextInterpretable()) {
            elementWidth = node.getSingleLineWidth(config);
            if (elementWidth < 0) {
                singleLineWidth = -1;
                return singleLineWidth;
            }
            singleLineWidth += elementWidth;
        }
        return super.getSingleLineWidth(config);
    }

}
