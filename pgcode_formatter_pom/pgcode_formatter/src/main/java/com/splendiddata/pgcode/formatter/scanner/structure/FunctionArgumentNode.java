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
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Declaration of a function argument
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class FunctionArgumentNode extends SrcNode {
    private static final Logger log = LogManager.getLogger(FunctionArgumentNode.class);

    private String mode;
    private final String name;
    private final String dataType;
    private final String defaultIndicator;
    private final ScanResult defaultExpr;
    
    /**
     * Constructor
     *
     * @param startNode The first word of the argument. This can be the mode, the argument name or the data type
     */
    public FunctionArgumentNode(ScanResult startNode) {
        super(ScanResultType.INTERPRETED, PostgresInputReader.interpretStatementBody(startNode));

        ScanResult lastInterpreted = getStartScanResult();
        ScanResult priorNode = lastInterpreted;
        ScanResult currentNode;

        for (priorNode = lastInterpreted;; currentNode = priorNode.getNext()) {
            currentNode = priorNode.getNext();
            if (currentNode == null || currentNode.isStatementEnd()
                    || currentNode.is(ScanResultType.CLOSING_PARENTHESIS)
                    || (currentNode.is(ScanResultType.CHARACTER) && ",".equals(currentNode.toString()))) {
                break;
            }
            currentNode = PostgresInputReader.interpretStatementBody(currentNode);
            priorNode.setNext(currentNode);
            priorNode = currentNode;
            if (currentNode.getType().isInterpretable()) {
                lastInterpreted = currentNode;
            }
        }

        setNext(lastInterpreted.getNext());
        lastInterpreted.setNext(null);

        /*
         * Mode (if any)
         */
        currentNode = getStartScanResult();
        switch (currentNode.toString().toLowerCase()) {
        case "in":
        case "out":
        case "inout":
        case "variadic":
            mode = currentNode.toString();
            currentNode = currentNode.getNextInterpretable();
            break;
        default:
            mode = "";
            break;
        }

        /*
         * name
         */
        IdentifierNode nameOrDataType = null;
        if (currentNode == null) {
            name = "";
            dataType = "";
            defaultIndicator = "";
            defaultExpr = null;
            return;
        } else if (currentNode.is(ScanResultType.IDENTIFIER)) {
            nameOrDataType = (IdentifierNode) currentNode;
            currentNode = currentNode.getNextInterpretable();
            if (currentNode == null) {
                name = "";
                dataType = nameOrDataType.toString();
                defaultIndicator = "";
                defaultExpr = null;
                return;
            }
            switch (currentNode.toString().toLowerCase()) {
            case "=":
            case "default":
                name = "";
                dataType = nameOrDataType.toString();
                defaultIndicator = currentNode.toString();
                defaultExpr = currentNode.getNextInterpretable();
                return;
            default:
                name = nameOrDataType.toString();
                nameOrDataType.setNotKeyword(true);
                break;
            }
        } else {
            name = "";
        }

        /*
         * According to postgres documentation:
         * For compatibility with some other database systems, argument mode can be written
         * either before or after argument name. But only the first way is standard-compliant.
         * So try again to find Mode (if any)
         */
        switch (currentNode.toString().toLowerCase()) {
            case "in":
            case "out":
            case "inout":
            case "variadic":
                mode = currentNode.toString();
                currentNode = currentNode.getNextInterpretable();
                break;
        }

        StringBuilder buf = new StringBuilder(currentNode.toString());
        for (currentNode = currentNode.getNextInterpretable(); currentNode != null
                && !"=".equals(currentNode.toString())
                && !"default".equalsIgnoreCase(currentNode.toString()); currentNode = currentNode
                        .getNextInterpretable()) {
            buf.append(" ").append(currentNode.toString());
        }
        dataType = buf.toString();

        if (currentNode != null
                && ("=".equals(currentNode.toString()) || "default".equalsIgnoreCase(currentNode.toString()))) {
            defaultIndicator = currentNode.toString();
            defaultExpr = currentNode.getNextInterpretable();
        } else {
            defaultIndicator = "";
            defaultExpr = null;
        }
        log.debug(() -> new StringBuilder().append("created: <").append(this).append(" >, mode=").append(mode)
                .append(", name=").append(name).append(", dataType=").append(dataType).append(", defaultIndicator=")
                .append(defaultIndicator).append(", defaultExpr=").append(defaultExpr));
    }

    /**
     * @return String the mode
     */
    public String getMode() {
        return mode;
    }

    /**
     * @return String the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return String the dataType
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * @return String the defaultIndicator
     */
    public String getDefaultIndicator() {
        return defaultIndicator;
    }

    /**
     * @return ScanResult the defaultExpr
     */
    public ScanResult getDefaultExpr() {
        return defaultExpr;
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
    public RenderMultiLines beautify(FormatContext formatContext, RenderMultiLines parentResult, FormatConfiguration config) {
        ArgumentDefinitionOffsets argumentDefinitionOffsets = formatContext.getArgumentDefinitionOffsets();
        log.debug(() -> new StringBuilder().append("beautify <").append(this).append(">, offsets=")
                .append(argumentDefinitionOffsets));
        RenderMultiLines result = new RenderMultiLines(this, formatContext, parentResult);
        ScanResult node = getStartScanResult();

        int parentOffset = 0;
        if (parentResult != null) {
            parentOffset = parentResult.getPosition();
        }
        /*
         * mode (if any)
         */
        if (!"".equals(mode)) {
            if (mode.equalsIgnoreCase(node.toString())) {
                result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
                for (node = node.getNext(); node != null && !node.getType().isInterpretable(); node = node.getNext()) {
                    result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
                }
            } else {
                result.addRenderResult(new IdentifierNode(mode).beautify(formatContext, result, config), formatContext);
                result.addWhiteSpaceIfApplicable();
            }
        }

        /*
         * argument name (if any)
         */
        if (node != null && !"".equals(name)) {
            if (argumentDefinitionOffsets != null && argumentDefinitionOffsets.getNameOffset() != null) {
                result.positionAt(parentOffset + argumentDefinitionOffsets.getNameOffset().intValue());
            }
            result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
            for (node = node.getNext(); node != null && !node.getType().isInterpretable(); node = node.getNext()) {
                result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
            }
        }

        /*
         * mode (if any)
         * Skip mode as it has already been added
         */
        if (!"".equals(mode) && mode.equalsIgnoreCase(node.toString())) {
            for (node = node.getNext(); node != null && !node.getType().isInterpretable(); node = node.getNext()) {
                result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
            }
        }

        /*
         * data type
         */
        if (node != null) {
            RenderMultiLines dataTypeResult = new RenderMultiLines(null, formatContext, parentResult);
            dataTypeResult.addRenderResult(node.beautify(formatContext, result, config), formatContext);
            for (node = node.getNext(); node != null && defaultIndicator != node.toString(); node = node.getNext()) {
                dataTypeResult.addRenderResult(node.beautify(formatContext, result, config), formatContext);
            }
            if (argumentDefinitionOffsets == null || argumentDefinitionOffsets.getDataTypeOffset() == null) {
                if (result.getPosition() + dataTypeResult.getWidth() > formatContext.getAvailableWidth()) {
                    result.addLine();
                }
            } else {
                if (argumentDefinitionOffsets.getDataTypeOffset().intValue() + dataTypeResult.getWidth() > formatContext
                        .getAvailableWidth()) {
                    result.addLine();
                } else {
                    result.positionAt(parentOffset + argumentDefinitionOffsets.getDataTypeOffset().intValue());
                }
            }
            result.addRenderResult(dataTypeResult, formatContext);
        }

        /*
         * default indicator (if any)
         */
        if (node != null) {
            if (argumentDefinitionOffsets != null && argumentDefinitionOffsets.getDefaultIndicatorOffset() != null) {
                result.positionAt(parentOffset + argumentDefinitionOffsets.getDefaultIndicatorOffset().intValue());
            } else {
                result.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
            }
            result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
            for (node = node.getNext(); node != null && !node.getType().isInterpretable(); node = node.getNext()) {
                result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
            }
        }

        /*
         * default expression (if any)
         */
        if (node != null) {
            RenderMultiLines defaultExpressionResult = new RenderMultiLines(null, formatContext, parentResult).setIndent(config.getStandardIndent());
            FormatContext defaultExpressionContext = new FormatContext(config, formatContext)
                    .setAvailableWidth(formatContext.getAvailableWidth() - config.getStandardIndent());
            for (; node != null; node = node.getNext()) {
                defaultExpressionResult.addRenderResult(node.beautify(defaultExpressionContext, defaultExpressionResult, config), formatContext);
            }
            if (argumentDefinitionOffsets == null || argumentDefinitionOffsets.getDefaultExpressionOffset() == null) {
                if (result.getPosition() + defaultExpressionResult.getWidth() > formatContext.getAvailableWidth()) {
                    result.addLine();
                }
            } else {
                if (argumentDefinitionOffsets.getDefaultExpressionOffset().intValue()
                        + defaultExpressionResult.getWidth() > formatContext.getAvailableWidth()) {
                    result.addLine();
                } else {
                    result.positionAt(parentOffset + argumentDefinitionOffsets.getDefaultExpressionOffset().intValue());
                }
            }
            result.addRenderResult(defaultExpressionResult, formatContext);
        }

        log.debug(() -> "render result = \n" + result.beautify());
        return result;
    }
}
