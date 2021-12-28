/*
 * Copyright (c) Splendid Data Product Development B.V. 2020 - 2021
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

import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.FunctionDefinitionArgumentGroupingType;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A create function class for: create [ or replace ] function ... create [ or replace ] procedure ...
 */
public class CreateFunctionNode extends SrcNode {
    private static final Logger log = LogManager.getLogger(CreateFunctionNode.class);

    private SrcNode language;

    /**
     * Constructor.
     * 
     * @param startNode
     *            The node that contains "create"
     */
    public CreateFunctionNode(ScanResult startNode) {
        super(ScanResultType.FUNCTION_DEFINITION, PostgresInputReader.toIdentifier(startNode));

        /*
         * The word CREATE
         */
        ScanResult lastInterpreted = getStartScanResult();

        /*
         * [ OR REPLACE ] { FUNCTION | PROCEDURE }
         */
        ScanResult priorNode = lastInterpreted.locatePriorToNextInterpretable();
        ScanResult currentNode = PostgresInputReader.toIdentifier(priorNode.getNext());
        priorNode.setNext(currentNode);
        lastInterpreted = currentNode;
        if (currentNode != null && "or".equalsIgnoreCase(currentNode.toString())) {
            priorNode = currentNode.locatePriorToNextInterpretable();
            currentNode = PostgresInputReader.toIdentifier(priorNode.getNext());
            priorNode.setNext(currentNode);
            lastInterpreted = currentNode;
            if (currentNode != null && "replace".equalsIgnoreCase(currentNode.toString())) {
                priorNode = currentNode.locatePriorToNextInterpretable();
                currentNode = PostgresInputReader.toIdentifier(priorNode.getNext());
                priorNode.setNext(currentNode);
                lastInterpreted = currentNode;
            }
        }

        /*
         * The function name
         */
        if (currentNode != null) {
            priorNode = currentNode.locatePriorToNextInterpretable();
            currentNode = PostgresInputReader.toIdentifier(priorNode.getNext());
            priorNode.setNext(currentNode);
            if (currentNode != null) {
                lastInterpreted = currentNode;
            }
        }

        /*
         * argument(s)
         */
        if (currentNode != null) {
            priorNode = currentNode.locatePriorToNextInterpretable();
            currentNode = priorNode.getNext();
            if (currentNode == null || !currentNode.is(ScanResultType.OPENING_PARENTHESIS)) {
                setNext(lastInterpreted.getNext());
                lastInterpreted.setNext(null);
                return;
            }
            currentNode = new InParentheses(currentNode, node -> CommaSeparatedList.ofDistinctElementTypes(node,
                    elementNode -> new FunctionArgumentNode(elementNode)));
            priorNode.setNext(currentNode);
            lastInterpreted = currentNode;
        }

        for (priorNode = currentNode.locatePriorToNextInterpretable();; priorNode = currentNode
                .locatePriorToNextInterpretable()) {
            currentNode = priorNode.getNext();

            if (currentNode == null || currentNode.isEof()) {
                break;
            }
            if (currentNode.is(ScanResultType.SEMI_COLON)) {
                lastInterpreted = new SemiColonNode(currentNode);
                priorNode.setNext(lastInterpreted);
                break;
            }
            if (currentNode.is(ScanResultType.IDENTIFIER)) {
                switch (currentNode.toString().toLowerCase()) {
                case "as":
                    currentNode = PostgresInputReader.toIdentifier(currentNode);
                    priorNode.setNext(currentNode);
                    lastInterpreted = currentNode;
                    priorNode = currentNode.locatePriorToNextInterpretable();
                    // AS 'definition'
                    currentNode = CommaSeparatedList.ofDistinctElementTypes(priorNode.getNext(),
                            node -> new FunctionDefinitionNode(node));
                    priorNode.setNext(currentNode);
                    lastInterpreted = currentNode;
                    break;
                case "language":
                    currentNode = PostgresInputReader.toIdentifier(currentNode);
                    priorNode.setNext(currentNode);
                    lastInterpreted = currentNode;
                    priorNode = currentNode.locatePriorToNextInterpretable();
                    language = PostgresInputReader.interpretStatementBody(priorNode.getNext());
                    priorNode.setNext(language);
                    lastInterpreted = language;
                    currentNode = language;
                    break;
                case "transform":
                    currentNode = PostgresInputReader.toIdentifier(currentNode);
                    priorNode.setNext(currentNode);
                    lastInterpreted = currentNode;
                    priorNode = currentNode.locatePriorToNextInterpretable();
                    currentNode = CommaSeparatedList.withArbitraryEnd(priorNode.getNext(),
                            node -> PostgresInputReader.interpretStatementBody(node), node -> {
                                if (!node.is(ScanResultType.IDENTIFIER)) {
                                    return false;
                                }
                                switch (node.toString().toLowerCase()) {
                                case "language":
                                case "window":
                                case "immutable":
                                case "stable":
                                case "volatile":
                                case "not":
                                case "leakproof":
                                case "external":
                                case "security":
                                case "parallel":
                                case "cost":
                                case "rows":
                                case "support":
                                case "set":
                                case "as":
                                    return true;
                                default:
                                    return false;
                                }
                            });
                    priorNode.setNext(currentNode);
                    lastInterpreted = currentNode;
                    break;
                default:
                    currentNode = PostgresInputReader.toIdentifier(currentNode);
                    priorNode.setNext(currentNode);
                    lastInterpreted = currentNode;
                    break;
                }
            } else {
                currentNode = PostgresInputReader.interpretStatementBody(currentNode);
                priorNode.setNext(currentNode);
                if (currentNode.getType().isInterpretable()) {
                    lastInterpreted = currentNode;
                }
            }
        }

        setNext(lastInterpreted.getNext());
        lastInterpreted.setNext(null);
    }

    /**
     * Returns the language that the function is implemented in. It can be sql, plpgsql, C ...etc.
     * 
     * @return The language that the function is implemented in.
     */
    public String getLanguage() {
        String result = null;
        if (language instanceof LiteralNode) {
            result = ((LiteralNode) language).getLiteral();
        } else if (language != null) {
            result = language.toString();
        }
        return result;
    }

    /**
     * @see Object#toString()
     *
     * @return String for debugging purposes
     */
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
     * @see ScanResult#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        /*
         * how do we format the arguments
         */
        FunctionDefinitionArgumentGroupingType argumentListConfig = config.getFunctionDefinitionArgumentGrouping();
        ArgumentDefinitionOffsets argumentDefinitionOffsets = getArgumentDefinitionOffsets(argumentListConfig);

        RenderMultiLines result = new RenderMultiLines(this, formatContext).setIndent(0);

        formatContext.setLanguage(getLanguage());
        FormatContext context = new FormatContext(config, formatContext);
        context.setLanguage(getLanguage());
        for (ScanResult current = getStartScanResult(); current != null; current = current.getNext()) {
            switch (current.getType()) {
            case IDENTIFIER:
                RenderResult renderResult = current.beautify(context, result, config);
                switch (current.toString().toLowerCase()) {
                case "as":
                    result.addLine();
                    result.addRenderResult(renderResult, formatContext);
                    result.addWhiteSpaceIfApplicable();
                    for (current = current.getNext(); current != null; current = current.getNext()) {
                        if (current instanceof CommaSeparatedList) {
                            /*
                             * The AS clause in a function definition usually has one literal: the code.
                             */
                            List<ListElement> code = ((CommaSeparatedList) current).getElements();
                            if (code.size() == 1) {
                                //                                /*
                                //                                 * List elements that are created with BeforeOrAfterType.BEFORE are indented with two
                                //                                 * spaces to allow room for the comma and a space. But we don't want that here, so let's
                                //                                 * tell it the comma (there will not be any) is supposed to occur AFTER it
                                //                                 */
                                //                                CommaSeparatedListGroupingType csListgrouping = new ObjectFactory()
                                //                                        .createCommaSeparatedListGroupingType();
                                //                                context.getCommaSeparatedListGrouping().copyTo(csListgrouping);
                                //                                csListgrouping.setCommaBeforeOrAfter(BeforeOrAfterType.AFTER);
                                //                                context.setCommaSeparatedListGrouping(csListgrouping);

                                result.addRenderResult(code.get(0).beautify(context, result, config), formatContext);
                            } else {
                                result.addRenderResult(current.beautify(formatContext, result, config), formatContext);
                            }
                            break;
                        }
                        result.addRenderResult(current.beautify(formatContext, result, config), formatContext);
                    }
                    break;
                case "not":
                    result.addLine();
                    result.addRenderResult(renderResult, formatContext);
                    result.addWhiteSpace();
                    if (current.getNextNonWhitespace() != null) {
                        current = current.getNextNonWhitespace();
                        result.addRenderResult(current.beautify(formatContext, result, config), formatContext);
                    }
                    break;
                case "language":
                case "transform":
                case "window":
                case "immutable":
                case "stable":
                case "volatile":
                case "leakproof":
                case "called":
                case "parallel":
                case "cost":
                case "rows":
                case "set":
                case "external":
                case "security":
                    result.addLine();
                    result.addRenderResult(renderResult, formatContext);
                    break;
                case "returns":
                    result.addLine();
                    result.addRenderResult(renderResult, formatContext);
                    break;
                case "table":
                    result.removeTrailingLineFeeds();
                    result.addWhiteSpaceIfApplicable();
                    result.addRenderResult(renderResult, formatContext);
                    break;
                default:
                    result.addRenderResult(renderResult, formatContext);
                    break;
                }
                break;
            case IN_PARENTHESES:
                /*
                 * This can only be the argument list
                 */
                FormatContext argumentsContext = new FormatContext(config, formatContext)
                        .setCommaSeparatedListGrouping(argumentListConfig.getArgumentGrouping())
                        .setArgumentDefinitionOffsets(argumentDefinitionOffsets);
                result.addRenderResult(current.beautify(argumentsContext, result, config), formatContext);
                break;
            default:
                result.addRenderResult(current.beautify(context, result, config), formatContext);
            }
        }

        return result;
    }

    /**
     * Determines the offsets within the constituent parts of a function argument definition
     *
     * @param config
     *            The FunctionDefinitionArgumentGroupingType that describes how to format a function argument
     * @return ArgumentDefinitionOffsets for rendering
     */
    private ArgumentDefinitionOffsets getArgumentDefinitionOffsets(FunctionDefinitionArgumentGroupingType config) {
        ArgumentDefinitionOffsets result = new ArgumentDefinitionOffsets();

        /*
         * Find the arguments
         */
        ScanResult node;
        for (node = getStartScanResult(); node != null
                && !(node instanceof InParentheses); node = node.getNextInterpretable()) {
            // Just find the arguments
        }
        List<ListElement> arguments = Collections.emptyList();
        if (node instanceof InParentheses) {
            for (node = ((InParentheses) node).getStartScanResult(); node != null
                    && !(node instanceof CommaSeparatedList); node = node.getNextInterpretable()) {
                // Just find the arguments
            }
            if (node instanceof CommaSeparatedList) {
                arguments = ((CommaSeparatedList) node).getElements();
            }
        }

        /*
         * Determine the maximum lengths of the constituent parts of the arguments
         */
        int maxModeLength = 0;
        int maxNameLength = 0;
        int maxDataTypeLength = 0;
        int maxDefaultIndictorLength = 0;
        for (ListElement element : arguments) {
            for (node = element.getStartScanResult(); node != null
                    && !(node instanceof FunctionArgumentNode); node = node.getNext()) {
                // Just find the argument
            }
            if (node instanceof FunctionArgumentNode) {
                FunctionArgumentNode argument = (FunctionArgumentNode) node;
                int length = argument.getMode().length();
                if (length > maxModeLength) {
                    maxModeLength = length;
                }
                length = argument.getName().length();
                if (length > maxNameLength) {
                    maxNameLength = length;
                }
                length = argument.getDataType().length();
                if (length > maxDataTypeLength) {
                    maxDataTypeLength = length;
                }
                length = argument.getDefaultIndicator().length();
                if (length > maxDefaultIndictorLength) {
                    maxDefaultIndictorLength = length;
                }
            }
        }

        /*
         * Position of the argument name
         */
        int offset = maxModeLength;
        if (offset > 0) {
            offset++;
        }
        switch (config.getArgumentName().getAlignment()) {
        case AT_HORIZONTAL_POSITION:
            offset = config.getArgumentName().getMinPosition().intValue();
            result.setNameOffset(config.getArgumentName().getMinPosition());
            break;
        case SUBSEQUENT:
            break;
        case VERTICALLY_ALIGNED:
            if (offset <= config.getArgumentName().getMinPosition().intValue()) {
                offset = config.getArgumentName().getMinPosition().intValue();
                result.setNameOffset(config.getArgumentName().getMinPosition());
            } else if (offset >= config.getArgumentName().getMaxPosition().intValue()) {
                offset = config.getArgumentName().getMaxPosition().intValue();
                result.setNameOffset(config.getArgumentName().getMaxPosition());
            } else {
                result.setNameOffset(Integer.valueOf(offset));
            }
            break;
        default:
            assert false : "Unknown " + config.getArgumentName().getAlignment().getClass().getName() + " value: "
                    + config.getArgumentName().getAlignment();
            break;
        }

        /*
         * Position of the data type
         */
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
         * Position of the default indicator
         */
        offset += maxDataTypeLength + 1;
        switch (config.getDefaultValue().getAlignment()) {
        case AT_HORIZONTAL_POSITION:
            offset = config.getDefaultValue().getMinPosition().intValue();
            result.setDefaultIndicatorOffset(config.getDefaultValue().getMinPosition());
            break;
        case SUBSEQUENT:
            break;
        case VERTICALLY_ALIGNED:
            if (offset <= config.getDefaultValue().getMinPosition().intValue()) {
                offset = config.getDefaultValue().getMinPosition().intValue();
                result.setDefaultIndicatorOffset(config.getDefaultValue().getMinPosition());
            } else if (offset >= config.getDefaultValue().getMaxPosition().intValue()) {
                offset = config.getDefaultValue().getMaxPosition().intValue();
                result.setDefaultIndicatorOffset(config.getDefaultValue().getMaxPosition());
            } else {
                result.setDefaultIndicatorOffset(Integer.valueOf(offset));
            }
            break;
        default:
            assert false : "Unknown " + config.getDefaultValue().getAlignment().getClass().getName() + " value: "
                    + config.getDefaultValue().getAlignment();
            break;
        }

        switch (config.getDefaultIndicator()) {
        case ALTER_TO_DEFAULT:
            offset += "default".length() + 1;
            break;
        case ALTER_TO_EQUALS_SIGN:
            offset += "=".length() + 1;
            break;
        case AS_IS:
            offset += maxDefaultIndictorLength + 1;
            break;
        default:
            assert false : "Unknown " + config.getDefaultIndicator().getClass().getName() + " value: "
                    + config.getDefaultIndicator();
            break;
        }

        /*
         * Position of the default expression
         */
        switch (config.getDefaultValue().getAlignment()) {
        case AT_HORIZONTAL_POSITION:
            offset = config.getDefaultValue().getMinPosition().intValue();
            result.setDefaultExpressionOffset(config.getDefaultValue().getMinPosition());
            break;
        case SUBSEQUENT:
            break;
        case VERTICALLY_ALIGNED:
            if (offset <= config.getDefaultValue().getMinPosition().intValue()) {
                offset = config.getDefaultValue().getMinPosition().intValue();
                result.setDefaultExpressionOffset(config.getDefaultValue().getMinPosition());
            } else if (offset >= config.getDefaultValue().getMaxPosition().intValue()) {
                offset = config.getDefaultValue().getMaxPosition().intValue();
                result.setDefaultExpressionOffset(config.getDefaultValue().getMaxPosition());
            } else {
                result.setDefaultExpressionOffset(Integer.valueOf(offset));
            }
            break;
        default:
            assert false : "Unknown " + config.getDefaultValue().getAlignment().getClass().getName() + " value: "
                    + config.getDefaultValue().getAlignment();
            break;
        }

        log.debug(() -> "getArgumentDefinitionOffsets() = " + result);
        return result;
    }
}
