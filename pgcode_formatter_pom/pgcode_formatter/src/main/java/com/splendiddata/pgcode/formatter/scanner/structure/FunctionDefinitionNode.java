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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.CodeFormatterThreadLocal;
import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.FunctionDefinitionRenderItem;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.internal.Util;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultStringLiteral;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Base class for plsql functions and procedures
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class FunctionDefinitionNode extends SrcNode {
    private static final Logger log = LogManager.getLogger(FunctionDefinitionNode.class);

    private String codeDelimiter;
    private String functionDefinition;

    /**
     * Constructor
     *
     * @param node
     *            The literal that reflects the function body.
     *
     */
    public FunctionDefinitionNode(ScanResult node) {
        super(ScanResultType.FUNCTION_BODY, node);
        if (node instanceof ScanResultStringLiteral) {
            functionDefinition = node.getText();
            codeDelimiter = ((ScanResultStringLiteral) node).getQuoteString();
            CodeFormatterThreadLocal.setStatementEnd(codeDelimiter);
        } else if (ScanResultType.DOUBLE_QUOTED_IDENTIFIER.equals(node.getType())) {
            functionDefinition = new DoubleQuotedIdentifierNode(node).toString();
            codeDelimiter = "\"";
        } else {
            throw new IllegalArgumentException("The constructor of " + getClass().getName()
                    + " expects a literal or double quoted identifier node");
        }

        setNext(node.getNext());
        node.setNext(null);

    }

    /**
     * @see SrcNode#toString()
     *
     * @return String a text that comes close to the original test for debugging purposes
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (codeDelimiter != null) {
            result.append(codeDelimiter);
        }
        if (functionDefinition != null) {
            result.append(functionDefinition);
        }
        if (codeDelimiter != null) {
            result.append(codeDelimiter);
        }
        return result.toString();
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {

        RenderItem result = null;

        try {
            /**
             * In case of anonymous code block, the language clause is optional, if omitted, the default is plpgsql.
             */
            if (formatContext.getLanguage() == null || "plpgsql".equals(formatContext.getLanguage())
                    || "sql".equals(formatContext.getLanguage())) {

                Reader stringReader = new StringReader(functionDefinition);

                try (PostgresInputReader postgresInputReader = new PostgresInputReader(stringReader)) {
                    RenderResult renderResult;
                    RenderMultiLines intermediateResult = new RenderMultiLines(this, formatContext, null);
                    ScanResult startNode = postgresInputReader.getFirstResult();
                    while (startNode != null
                            && (startNode.is(ScanResultType.WHITESPACE) || startNode.is(ScanResultType.LINEFEED))) {
                        startNode = startNode.getNextNonWhitespace();
                    }
                    for (SrcNode nextNode = PostgresInputReader
                            .interpretPlpgsqlStatementStart(startNode); nextNode != null; nextNode = PostgresInputReader
                                    .interpretPlpgsqlStatementStart(nextNode.getNext())) {
                        if (nextNode instanceof PlpgsqlBeginEndBlock) {
                            intermediateResult.positionAt(0);
                        }
                        renderResult = nextNode.beautify(formatContext, intermediateResult, config);
                        intermediateResult.addRenderResult(renderResult, formatContext);
                    }
                    int height = intermediateResult.getHeight();
                    int width = intermediateResult.getWidth();
                    if (width < codeDelimiter.length()) {
                        width = codeDelimiter.length();
                    }
                    /*
                     * remove trailing spaces, tabs and linefeeds
                     */
                    StringBuilder resultText = new StringBuilder().append(codeDelimiter).append("\n")
                            .append(Util.performTabReplacement(config, intermediateResult.beautify()));
                    for (int i = resultText.length(); i > 0;) {
                        switch (resultText.charAt(--i)) {
                        case '\n':
                            if (height > 0) {
                                height--;
                            }
                            // no break
                        case ' ':
                        case '\t':
                            resultText.setLength(i);
                            break;
                        default:
                            i = -1;
                            break;
                        }
                    }
                    resultText.append("\n").append(codeDelimiter).toString();
                    height += 2; // for the delimiters
                    result = new FunctionDefinitionRenderItem(resultText.toString(), RenderItemType.FUNCTION_DEFINITION)
                            .setWidth(width).setHeight(height);
                }
            } else {
                String[] lines = this.toString().split("\n");
                int width = 0;
                for (String line : lines) {
                    if (line.length() > width) {
                        width = line.length();
                    }
                }
                result = new FunctionDefinitionRenderItem(this.toString(), RenderItemType.FUNCTION_DEFINITION)
                        .setWidth(width).setHeight(lines.length);
            }
        } catch (IOException e) {
            log.error(e, e);
        }

        return result;
    }

    /**
     * @see ScanResult#getSingleLineWidth(FormatConfiguration)
     *
     * @return -1 in all cases
     */
    @Override
    public int getSingleLineWidth(FormatConfiguration config) {
        return -1;
    }

}
