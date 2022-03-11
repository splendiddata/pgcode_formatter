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

import java.util.List;
import java.util.ListIterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.*;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A function call with arguments
 *
 * Note. This class will be also used in cases where an expression looks like a function call while is not a function
 * call: Examples: cursor declaration: cursor_name no scroll cursor(a,b) is select 1; variable declaration: number
 * numeric(5);
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */

public class FunctionCallNode extends SrcNode {
    private static final Logger log = LogManager.getLogger(FunctionCallNode.class);
    private int singleLineWidth;
    private RenderMultiLines singleLineResult;

    /**
     * Constructor
     *
     * @param functionName
     *            Name of the function
     */
    public FunctionCallNode(IdentifierNode functionName) {
        super(ScanResultType.FUNCTION_CALL, functionName);
        ScanResult priorNode = functionName.locatePriorToNextInterpretable();
        ScanResult currentNode = priorNode.getNext();
        if (currentNode != null && currentNode.is(ScanResultType.OPENING_PARENTHESIS)) {
            currentNode = new InParentheses(currentNode, argStart -> CommaSeparatedList.withArbitraryEnd(argStart,
                    argNode -> PostgresInputReader.interpretStatementBody(argNode), argNode -> false));
            priorNode.setNext(currentNode);
        } else {
            currentNode = priorNode;
        }
        this.setNext(currentNode.getNext());
        currentNode.setNext(null);
        log.debug(() -> this);
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {

        int availableWidth = formatContext.getAvailableWidth();
        getSingleLineWidth(config);
        if (singleLineWidth > 0 && singleLineWidth <= availableWidth && singleLineResult != null) {
            return singleLineResult.clone();
        }
        FormatContext itemContext = new FormatContext(config, formatContext)
                .setAvailableWidth(availableWidth - config.getStandardIndent());
        RenderMultiLines renderResult = new RenderMultiLines(this, formatContext, parentResult);
        RenderResult itemResult;

        // First format the function (qualified) name in a function call.
        ScanResult startScanResult = getStartScanResult();
        if (startScanResult instanceof QualifiedIdentifierNode) {
            RenderMultiLines result = new RenderMultiLines(startScanResult, formatContext, renderResult);
            List<ScanResult> allNameParts = ((QualifiedIdentifierNode) startScanResult).getAllNameParts();
            ListIterator<ScanResult> listIterator = allNameParts.listIterator();
            while (listIterator.hasNext()) {
                ScanResult node = listIterator.next();
                switch (node.getType()) {
                case WHITESPACE:
                case LINEFEED:
                    break;
                case COMMENT:
                    result.addWhiteSpace();
                    result.addRenderResult(node.beautify(new FormatContext(config, formatContext)
                            .setCommaSeparatedListGrouping(config.getFunctionCallArgumentGrouping()), result, config),
                            formatContext);
                    result.addWhiteSpace();
                    break;
                case IDENTIFIER:
                    result.addRenderResult(new RenderItem(pgBuiltInFunctionsToLetterCase(config, node.getText()), this,
                            RenderItemType.IDENTIFIER), formatContext);
                    break;
                case COMMENT_LINE:
                    result.addWhiteSpace();
                    //$FALL-THROUGH$
                default:
                    result.addRenderResult(node.beautify(new FormatContext(config, formatContext)
                            .setCommaSeparatedListGrouping(config.getFunctionCallArgumentGrouping()), result, config),
                            formatContext);
                }
            }
            itemResult = result;
        } else {
            itemResult = new RenderItem(pgBuiltInFunctionsToLetterCase(config, startScanResult.getText()), this,
                    RenderItemType.IDENTIFIER);
        }

        renderResult.addRenderResult(itemResult, formatContext);

        // Format the rest of the function call.
        for (ScanResult srcNode = getStartScanResult().getNext(); srcNode != null; srcNode = srcNode.getNext()) {
            renderResult.addRenderResult(srcNode.beautify(itemContext, renderResult, config), formatContext);
        }
        if (renderResult.getHeight() <= 1) {
            singleLineResult = renderResult.clone();
            singleLineWidth = renderResult.getWidth();
        }
        return renderResult;
    }

    /**
     * @see ScanResult#getSingleLineWidth(FormatConfiguration)
     */
    @Override
    public int getSingleLineWidth(FormatConfiguration config) {
        if (singleLineWidth != 0) {
            return singleLineWidth;
        }
        FormatConfiguration callConfig = new FormatConfiguration(config)
                .setCommaSeparatedListGrouping(config.getFunctionCallArgumentGrouping());
        int elementWidth;
        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            elementWidth = node.getSingleLineWidth(callConfig);
            if (elementWidth < 0) {
                singleLineWidth = -1;
                return singleLineWidth;
            }
            singleLineWidth += elementWidth;
            if (singleLineWidth > callConfig.getLineWidth().getValue()) {
                singleLineWidth = -1;
                return singleLineWidth;
            }
        }
        return singleLineWidth;
    }

    /**
     * Converts the token to upper/lower case or keeps it unchanged based on the provided configuration.
     *
     * @param config
     *            {@link FormatConfiguration}
     * @return The converted token (or the token if unchanged).
     */
    private String pgBuiltInFunctionsToLetterCase(FormatConfiguration config, String token) {
        if (Dicts.pgFunctions.contains(token.toUpperCase()) || "pg_catalog".equals(token.toLowerCase())
                || "information_schema".equals(token.toLowerCase())) {
            switch (config.getLetterCaseFunctions()) {
            case LOWERCASE:
                return token.toLowerCase();
            case UPPERCASE:
                return token.toUpperCase();
            case UNCHANGED:
            default:
                return token;
            }
        }
        return token;
    }
}
