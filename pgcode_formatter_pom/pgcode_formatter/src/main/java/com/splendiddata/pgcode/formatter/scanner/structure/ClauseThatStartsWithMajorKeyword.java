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

    private static final Set<String> LOGICAL_OPERATORS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("AND", "OR")));

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
        int availableWidth = formatContext.getAvailableWidth();
        int standardIndent = FormatContext.indent(true).length();
        /*
         * Try to render on a single line
         */

        if (!config.getQueryConfig().isMajorKeywordsOnSeparateLine().booleanValue()) {
            RenderMultiLines result = new RenderMultiLines(this, formatContext);
            for (ScanResult node = getStartScanResult(); node != null && result.getHeight() <= 1; node = node.getNext()) {
                result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
            }
            if (result.getHeight() == 1 && result.getPosition() <= availableWidth) {
                return result;
            }
        }

        /*
         * Single line didn't work out, so render multiline
         */
        RenderMultiLines result = new RenderMultiLines(this, formatContext);
        FormatContext contentContext = new FormatContext(config, formatContext)
                .setAvailableWidth(availableWidth - standardIndent);
        ScanResult node = getStartScanResult();
        result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
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
                            result.addRenderResult(node.beautify(contentContext, result, config), formatContext);
                        }
                    }
                } while (node != endOfKeyword);
            }
            result.addLine();
        } else {
            result.addWhiteSpace();
        }

        int indent = 0;
        switch (config.getLogicalOperatorsIndent().getIndent()) {
        case DOUBLE_INDENTED:
            indent = 2 * standardIndent;
            break;
        case INDENTED:
            indent = standardIndent;
            break;
        case UNDER_FIRST_ARGUMENT:
        default:
            indent = result.getPosition();
            break;
        }
        result.setIndent(indent);
        contentContext = new FormatContext(config, formatContext)
                .setAvailableWidth(availableWidth - indent);
        boolean passedBetweenKeyword = false;
        if (node != null) {
            for (node = node.getNext(); node != null; node = node.getNext()) {
                RenderResult itemResult = node.beautify(contentContext, result, config);
                if (result.getPosition() > indent
                        && result.getPosition() + itemResult.getWidth() > availableWidth) {
                    result.addLine();
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
                        result.addLine();
                        result.addRenderResult(itemResult, contentContext);
                        break;
                    default:
                        if (onSeparateLine) {
                            result.addLine();
                            result.addRenderResult(itemResult, contentContext);
                            result.addLine();
                        } else {
                            result.addRenderResult(itemResult, contentContext);
                        }
                        break;
                    }
                } else {
                    result.addRenderResult(itemResult, contentContext);
                }
                if ("AND".equalsIgnoreCase(node.toString())) {
                    passedBetweenKeyword = false;
                }
            }
        }

        formatContext.setAvailableWidth(availableWidth);
        return result;
    }

}
