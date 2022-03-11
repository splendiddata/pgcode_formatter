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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.splendiddata.pgcode.formatter.ConfigUtil;
import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.QueryConfigType;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Contains a default implementation for clauses that start with a major keyword (which might be rendered on a separate
 * line because of the {@link QueryConfigType#isMajorKeywordsOnSeparateLine()} setting
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public abstract class ClauseThatStartsWithMajorKeyword extends SrcNode implements WantsNewlineBefore {

    private static final Set<String> LOGICAL_OPERATORS = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("AND", "OR")));
    private int singleLineWidth;

    /**
     * Constructor
     *
     * @param type
     * @param scanResult
     */
    protected ClauseThatStartsWithMajorKeyword(ScanResultType type, ScanResult scanResult) {
        super(type, scanResult);
    }

    /**
     * Returns the IdentifierNode that marks the end of a major keyword. Usually this is the keyword itself, but
     * sometimes more than one word is considered part of the same "major keyword". Think for example of SELECT ALL or
     * SELECT DISTINCT or INTO STRICT ...
     * <p>
     * The default implementation just returns the startScanResult.
     *
     * @return IdentifierNode
     */
    protected IdentifierNode getEndOfMajorKeyword() {
        return (IdentifierNode) getStartScanResult();
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

        int availableWidth = formatContext.getAvailableWidth();
        int startPosition = 0;
        if (parentResult != null) {
            startPosition = parentResult.getPosition();
        }

        /*
         * Try to render on a single line
         */

        if (!config.getQueryConfig().isMajorKeywordsOnSeparateLine().booleanValue()) {
            int singleLineLength = getSingleLineWidth(config);
            if (singleLineLength > 0 && singleLineLength + startPosition < config.getLineWidth().getValue()) {
                renderResult = new RenderMultiLines(this, formatContext, parentResult);
                for (ScanResult node = getStartScanResult(); node != null
                        && renderResult.getHeight() <= 1; node = node.getNext()) {
                    renderResult.addRenderResult(node.beautify(formatContext, renderResult, config), formatContext);
                }
                if (renderResult.getHeight() == 1 && renderResult.getPosition() <= availableWidth) {
                    return cacheRenderResult(renderResult, formatContext, parentResult);
                }
            }
        }

        /*
         * Single line didn't work out, so render multiline
         */
        renderResult = new RenderMultiLines(this, formatContext, parentResult);
        renderResult.setIndentBase(renderResult.getPosition());
        FormatContext contentContext = new FormatContext(config, formatContext)
                .setAvailableWidth(availableWidth - config.getStandardIndent());
        ScanResult node = getStartScanResult();
        renderResult.addRenderResult(node.beautify(formatContext, renderResult, config), formatContext);
        if (config.getQueryConfig().isMajorKeywordsOnSeparateLine().booleanValue()) {
            /*
             * Sometimes a "major keyword" consists of more that one word. Think of SELECT DISTINCT or INTO STRICT. If
             * that is the case, then keep these words together
             */
            IdentifierNode endOfKeyword = getEndOfMajorKeyword();
            if (node != endOfKeyword) {
                do {
                    if (node != null) {
                        node = node.getNext();
                        if (node != null) {
                            renderResult.addRenderResult(node.beautify(contentContext, renderResult, config),
                                    formatContext);
                        }
                    }
                } while (node != endOfKeyword);
            }
            renderResult.addLine();
        } else {
            renderResult.addWhiteSpace();
        }

        int indent = 0;
        switch (config.getLogicalOperatorsIndent().getIndent()) {
        case DOUBLE_INDENTED:
            indent = 2 * config.getStandardIndent();
            break;
        case INDENTED:
            indent = config.getStandardIndent();
            break;
        case UNDER_FIRST_ARGUMENT:
        default:
            indent = renderResult.getPosition() - renderResult.getIndentBase();
            break;
        }
        renderResult.setIndent(indent);
        contentContext = new FormatContext(config, formatContext).setAvailableWidth(availableWidth - indent);
        boolean passedBetweenKeyword = false;
        if (node != null) {
            for (node = node.getNext(); node != null; node = node.getNext()) {
                RenderResult itemResult = node.beautify(contentContext, renderResult, config);
                if (renderResult.getPosition() > renderResult.getIndentBase() + indent
                        && renderResult.getPosition() + itemResult.getWidth() > availableWidth) {
                    renderResult.addLine();
                }
                // The case where AND is used in the BETWEEN predicate should be excluded (e.g. a BETWEEN x AND y).
                if (node instanceof IdentifierNode && "BETWEEN".equalsIgnoreCase(node.toString())) {
                    passedBetweenKeyword = true;
                }
                if (node instanceof IdentifierNode && !passedBetweenKeyword
                        && LOGICAL_OPERATORS.contains(node.toString().toUpperCase())) {
                    boolean onSeparateLine = config.getQueryConfig().isMajorKeywordsOnSeparateLine().booleanValue()
                            && ConfigUtil.isMajorKeywords(node.getText());
                    switch (config.getLogicalOperatorsIndent().getIndent()) {
                    case UNDER_FIRST_ARGUMENT:
                    case INDENTED:
                    case DOUBLE_INDENTED:
                        renderResult.addLine();
                        renderResult.addRenderResult(itemResult, contentContext);
                        break;
                    default:
                        if (onSeparateLine) {
                            renderResult.addLine();
                            renderResult.addRenderResult(itemResult, contentContext);
                            renderResult.addLine();
                        } else {
                            renderResult.addRenderResult(itemResult, contentContext);
                        }
                        break;
                    }
                } else {
                    renderResult.addRenderResult(itemResult, contentContext);
                }
                if ("AND".equalsIgnoreCase(node.toString())) {
                    passedBetweenKeyword = false;
                }
            }
        }

        formatContext.setAvailableWidth(availableWidth);
        return cacheRenderResult(renderResult, formatContext, parentResult);
    }

    /**
     * @see ScanResult#getSingleLineWidth(FormatConfiguration)
     */
    @Override
    public int getSingleLineWidth(FormatConfiguration config) {
        if (singleLineWidth == 0) {
            int elementWidth;
            for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
                elementWidth = node.getSingleLineWidth(config);
                if (elementWidth < 0) {
                    singleLineWidth = -1;
                    break;
                }
                singleLineWidth += elementWidth;
            }
        }
        return singleLineWidth;
    }

}
