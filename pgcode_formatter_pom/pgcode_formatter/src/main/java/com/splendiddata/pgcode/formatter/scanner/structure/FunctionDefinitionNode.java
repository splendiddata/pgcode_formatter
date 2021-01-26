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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.*;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.TabsOrSpacesType;
import com.splendiddata.pgcode.formatter.internal.*;
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
     *            Start node that contains the function/procedure definition, i.e. the part that starts after "AS" or
     *            starts after "DO" in case of anonymous code block.
     *
     */
    public FunctionDefinitionNode(ScanResult node) {
        super(ScanResultType.FUNCTION_BODY, node);
        ScanResult currentNode = node;
        ScanResult priorNode = null;
        while (!currentNode.getType().isInterpretable() && !currentNode.isEof()) {
            priorNode = currentNode;
            currentNode = currentNode.getNext();
        }
        if (currentNode instanceof ScanResultStringLiteral) {
            functionDefinition = currentNode.getText();
            codeDelimiter = ((ScanResultStringLiteral) currentNode).getQuoteString();
            CodeFormatterThreadLocal.setStatementEnd(codeDelimiter);
        } else if (ScanResultType.DOUBLE_QUOTED_IDENTIFIER.equals(currentNode.getType())) {
            functionDefinition = new DoubleQuotedIdentifierNode(currentNode).toString();
            codeDelimiter = "\"";
        } else {
            functionDefinition = "";
            setNext(currentNode);
            if (priorNode != null) {
                priorNode.setNext(null);
            }
            return;
        }

        setNext(currentNode.getNext());
        currentNode.setNext(null);

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
        RenderMultiLines result = new RenderMultiLines(this, formatContext).setIndent(0);

        Pattern tabSplitPattern = TabsOrSpacesType.TABS.equals(config.getTabs().getTabsOrSpaces())
                ? Pattern.compile("(\n)|([^\n]{1," + config.getTabs().getTabWidth() + "})")
                : null;
        Pattern tabReplacementPattern = tabSplitPattern == null ? null : Pattern.compile("\\s{2,}$");

        /*
         * If only the indent is to be replaced by tabs, then the leadingSpacesPattern will be filled to help replacing
         * leading spaces by tabs
         */
        Pattern leadingSpacesPattern = tabSplitPattern == null
                && TabsOrSpacesType.TABS.equals(config.getIndent().getTabsOrSpaces())
                        ? Pattern.compile("\\n([\\s^\\n]+)")
                        : null;

        try {
            /**
             * In case of anonymous code block, the language clause is optional, if omitted, the default is plpgsql.
             */
            if (formatContext.getLanguage() == null || "plpgsql".equals(formatContext.getLanguage())) {

                Reader stringReader = new StringReader(functionDefinition);

                try (PostgresInputReader postgresInputReader = new PostgresInputReader(stringReader)) {
                    RenderResult renderResult;
                    RenderItem codeDelimiterItem = null;
                    if (codeDelimiter != null) {
                        codeDelimiterItem = new RenderItem(codeDelimiter, RenderItemType.LITERAL);
                        result.addRenderResult(codeDelimiterItem, formatContext);
                        result.addLine();

                    }

                    for (SrcNode nextNode = PostgresInputReader.interpretPlpgsqlStatementStart(
                            postgresInputReader.getFirstResult()); nextNode != null; nextNode = PostgresInputReader
                                    .interpretPlpgsqlStatementStart(nextNode.getNext())) {
                        renderResult = nextNode.beautify(formatContext, result, config);
                        if (nextNode instanceof WantsNewlineBefore) {
                            result.addLine();
                        }
                        result.addRenderResult(renderResult, formatContext);
                    }

                    if (codeDelimiter != null) {
                        result.addLine();
                        result.addRenderResult(new RenderItem(codeDelimiter, RenderItemType.LITERAL), formatContext);
                    }

                    /**
                     * The function definition beautification is complete now, so cash the height and width
                     */
                    int height = result.getHeight();
                    int width = result.getWidth();

                    if (codeDelimiter != null) {
                        result.removeFirst();

                        RenderResult last = result.getLast();
                        if (RenderItemType.LITERAL.equals(last.getRenderItemType())
                                && codeDelimiter.equalsIgnoreCase(codeDelimiterItem.getNonBreakableText())) {
                            result.removeLast();
                        }
                    }

                    String functionBody = result.beautify();

                    if (tabSplitPattern != null) {
                        functionBody = Util.replaceSpacesByTabs(config, tabSplitPattern, tabReplacementPattern,
                                functionBody);
                    } else if (leadingSpacesPattern != null) {
                        functionBody = Util.replaceLeadingSpaces(config, leadingSpacesPattern, functionBody);
                    }

                    if (codeDelimiter != null) {
                        functionBody = codeDelimiter + functionBody + codeDelimiter;

                    }

                    FunctionDefinitionRenderItem item = new FunctionDefinitionRenderItem(functionBody,
                            RenderItemType.LITERAL);
                    item.setHeight(height);
                    item.setWidth(width);

                    return item;
                }
            } else if ("sql".equals(formatContext.getLanguage())) {
                RenderItem codeDelimiterItem = null;

                if (codeDelimiter != null) {
                    codeDelimiterItem = new RenderItem(codeDelimiter, RenderItemType.LITERAL);
                    result.addRenderResult(codeDelimiterItem, formatContext);
                    result.addLine();
                }

                Util.toRenderResults(new StringReader(functionDefinition), config)
                        .forEach(statement -> result.addRenderResult(statement, formatContext));
                if (codeDelimiter != null) {
                    result.addLine();
                    result.addRenderResult(new RenderItem(codeDelimiter, RenderItemType.LITERAL), formatContext);
                }

                int height = result.getHeight();
                int width = result.getWidth();

                if (codeDelimiter != null) {
                    result.removeFirst();

                    RenderResult last = result.getLast();
                    if (RenderItemType.LITERAL.equals(last.getRenderItemType())
                            && codeDelimiter.equalsIgnoreCase(codeDelimiterItem.getNonBreakableText())) {
                        result.removeLast();
                    }
                }

                String functionBody = result.beautify();
                if (tabSplitPattern != null) {
                    functionBody = Util.replaceSpacesByTabs(config, tabSplitPattern, tabReplacementPattern, functionBody);
                } else if (leadingSpacesPattern != null) {
                    functionBody = Util.replaceLeadingSpaces(config, leadingSpacesPattern, functionBody);
                }

                if (codeDelimiter != null) {
                    functionBody = codeDelimiter + functionBody + codeDelimiter;

                }

                FunctionDefinitionRenderItem item = new FunctionDefinitionRenderItem(functionBody,
                        RenderItemType.LITERAL);
                item.setHeight(height);
                item.setWidth(width);

                return item;

            } else {
                RenderItem renderItem = new RenderItem(this.toString(), this, RenderItemType.FUNCTION_DEFINITION);
                result.addRenderResult(renderItem, formatContext);
            }
        } catch (IOException e) {
            log.error(e, e);
        }

        return result;
    }
}
