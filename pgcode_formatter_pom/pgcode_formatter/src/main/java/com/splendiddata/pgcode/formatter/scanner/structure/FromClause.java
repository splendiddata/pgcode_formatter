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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.ConfigUtil;
import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.BeforeOrAfterType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CommaSeparatedListGroupingType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.FromItemGroupingType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.RelativePositionTypeEnum;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.Util;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * The FROM clause of a select statement
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class FromClause extends SrcNode implements WantsNewlineBefore {
    private static final Logger log = LogManager.getLogger(FromClause.class);

    private static final Set<String> JOIN_WORDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("NATURAL", "CROSS", "LEFT", "RIGHT", "FULL", "INNER", "OUTER", "JOIN")));
    private static final Set<String> KEY_WORDS = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("NATURAL", "CROSS", "LEFT", "RIGHT", "FULL", "INNER", "OUTER",
                    "JOIN", "LATERAL", "WITH", "ORDINARY", "TABLESAMPLE", "ROWS", "AS", "ON")));

    /**
     * Constructor
     *
     * @param startNode
     *            The word FROM that starts the FROM clause
     */
    public FromClause(ScanResult startNode) {
        super(ScanResultType.INTERPRETED, new IdentifierNode(startNode));
        assert "from".equalsIgnoreCase(
                startNode.toString()) : "The from clause is supposed with the word FROM, not with: " + startNode;

        int parenthesesLevel = startNode.getParenthesisLevel();

        ScanResult lastInterpreted = getStartScanResult();
        ScanResult priorNode;
        ScanResult currentNode;
        for (priorNode = lastInterpreted.locatePriorToNextInterpretable();; priorNode = currentNode
                .locatePriorToNextInterpretable()) {
            currentNode = priorNode.getNext();
            if (currentNode == null || currentNode.isStatementEnd()
                    || currentNode.getParenthesisLevel() < parenthesesLevel || isComplete(currentNode)) {
                break;
            }
            currentNode = PostgresInputReader.interpretStatementBody(currentNode);
            priorNode.setNext(currentNode);
            if (currentNode.getType().isInterpretable()) {
                lastInterpreted = currentNode;
            }
        }

        setNext(lastInterpreted.getNext());
        lastInterpreted.setNext(null);
        log.debug(() -> "cunstructed " + this);
    }

    private static boolean isComplete(ScanResult node) {
        if (!node.is(ScanResultType.IDENTIFIER)) {
            return false;
        }
        switch (node.toString().toLowerCase()) {
        case "where":
        case "group":
        case "having":
        case "window":
        case "union":
        case "intersect":
        case "except":
        case "order":
        case "limit":
        case "offset":
        case "fetch":
        case "for":
        case "into":
            return true;
        default:
            return false;
        }
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderMultiLines beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {

        RenderMultiLines result = new RenderMultiLines(this, formatContext).setIndent(0);
        ScanResult node = getStartScanResult();
        result.addRenderResult(node.beautify(formatContext, result, config), formatContext); // from
        if (config.getQueryConfig().isMajorKeywordsOnSeparateLine().booleanValue()) {
            result.addLine();
        } else {
            result.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
        }
        result.addRenderResult(renderContent(node.getNextNonWhitespace(), formatContext, config), formatContext);

        if (log.isDebugEnabled()) {
            log.debug("beautify result =\n" + result.beautify());
        }
        return result;
    }

    /**
     * Renders everything after FROM
     *
     * @param firstNonWhitespace
     *            The start of the first table entry
     * @param formatContext
     *            To help rendering
     * @param config
     *            With the specs
     * @return RenderMultiLines with the content
     */
    private RenderMultiLines renderContent(ScanResult firstNonWhitespace, FormatContext formatContext,
            FormatConfiguration config) {
        FromItemGroupingType fromConfig = config.getFromItemGrouping();
        CommaSeparatedListGroupingType csListConfig = ConfigUtil.copy(config.getCommaSeparatedListGrouping());
        csListConfig.setMultilineOpeningParenBeforeArgument(fromConfig.isMultilineOpeningParenBeforeArgument());
        csListConfig.setMultilineClosingParenOnNewLine(fromConfig.isMultilineClosingParenOnNewLine());
        csListConfig.setMaxSingleLineLength(fromConfig.getMaxSingleLineLength());
        FormatContext myContext = new FormatContext(config, formatContext).setCommaSeparatedListGrouping(csListConfig)
                .setAvailableWidth(formatContext.getAvailableWidth() - 5);
        int maxLength = fromConfig.getMaxSingleLineLength().getValue();
        if (maxLength > formatContext.getAvailableWidth() && config.getLineWidth().getWeight()
                .floatValue() >= fromConfig.getMaxSingleLineLength().getWeight().floatValue()) {
            maxLength = formatContext.getAvailableWidth();
        }
        boolean containsComma = false;

        RenderMultiLines result = new RenderMultiLines(null, formatContext);
        /*
         * First try to render everything on a single line (while gathering render results that will be used if
         * rendering on a single line didn't work out)
         */
        for (ScanResult node = firstNonWhitespace; node != null; node = node.getNext()) {
            if (node.is(ScanResultType.CHARACTER) && ",".equals(node.toString())) {
                containsComma = true;
                result.removeTrailingSpaces();
                result.addRenderResult(node.beautify(myContext, result, config), formatContext);
                result.addWhiteSpace();
            } else {
                result.addRenderResult(node.beautify(myContext, result, config), formatContext);
            }
        }
        if (result.getHeight() <= 1 && result.getWidth() <= maxLength) {
            // The result fits on a single line
            if (log.isTraceEnabled()) {
                log.trace(new StringBuilder().append("beautify single line: ").append(this).append("\nconfig: ")
                        .append(Util.xmlBeanToString(fromConfig)).append("=\n").append(result.beautify()));
            }
            return result;
        }

        /*
         * The result didn't fit on a single line, so render multiline
         */
        result = new RenderMultiLines(null, formatContext).setIndent(decideOnIndent(config, containsComma));
        RenderMultiLines tableEntryResult = new RenderMultiLines(null, formatContext).setIndent(0);
        if (RelativePositionTypeEnum.SUBSEQUENT.equals(fromConfig.getAliasAlignment().getAlignment())) {
            // No need to take care of positioning the alias correctly
            for (ScanResult node = firstNonWhitespace; node != null; node = node.getNext()) {
                if (node.is(ScanResultType.CHARACTER) && ",".equals(node.toString())) {
                    tableEntryResult.removeTrailingSpaces();
                    result.addRenderResult(tableEntryResult, formatContext);
                    nextElementOnNextLine(result, fromConfig.getComma(), formatContext);
                    tableEntryResult = new RenderMultiLines(null, formatContext);
                } else if (node.is(ScanResultType.IDENTIFIER) && JOIN_WORDS.contains(node.toString().toUpperCase())) {
                    result.addRenderResult(tableEntryResult, formatContext);
                    result.addLine();
                    tableEntryResult = new RenderMultiLines(null, formatContext);
                    for (; (node.is(ScanResultType.IDENTIFIER) && JOIN_WORDS.contains(node.toString().toUpperCase()))
                            || !node.getType().isInterpretable(); node = node.getNext()) {
                        tableEntryResult.addRenderResult(node.beautify(myContext, tableEntryResult, config),
                                formatContext);
                    }
                    tableEntryResult.addRenderResult(node.beautify(myContext, tableEntryResult, config), formatContext);
                } else {
                    tableEntryResult.addRenderResult(node.beautify(myContext, tableEntryResult, config), formatContext);
                }
            }
            result.addRenderResult(tableEntryResult, formatContext);
            if (log.isTraceEnabled()) {
                log.trace(new StringBuilder().append("beautify subsequent alias positioning: ").append(this)
                        .append("\nconfig: ").append(Util.xmlBeanToString(fromConfig)).append("=\n")
                        .append(result.beautify()));
            }
            return result;
        }

        /*
         * The alias position is important here
         */
        int aliasPosition = fromConfig.getAliasAlignment().getMinPosition().intValue();
        Map<ScanResult, ScanResult> aliases = new IdentityHashMap<>();
        boolean passedANonKeyword = false;
        boolean aliasFound = false;
        for (ScanResult node = firstNonWhitespace; node != null; node = node.getNext()) {
            if (node.is(ScanResultType.CHARACTER) && ",".equals(node.toString())) {
                tableEntryResult.removeTrailingSpaces();
                result.addRenderResult(tableEntryResult, formatContext);
                nextElementOnNextLine(result, fromConfig.getComma(), formatContext);
                tableEntryResult = new RenderMultiLines(null, formatContext);
                passedANonKeyword = false;
                aliasFound = false;
            } else if (node.is(ScanResultType.IDENTIFIER) && JOIN_WORDS.contains(node.toString().toUpperCase())) {
                tableEntryResult.removeTrailingSpaces();
                result.addRenderResult(tableEntryResult, formatContext);
                result.addLine();
                tableEntryResult = new RenderMultiLines(null, formatContext);
                for (; (node.is(ScanResultType.IDENTIFIER) && JOIN_WORDS.contains(node.toString().toUpperCase()))
                        || !node.getType().isInterpretable(); node = node.getNext()) {
                    tableEntryResult.addRenderResult(node.beautify(myContext, tableEntryResult, config), formatContext);
                }
                passedANonKeyword = true;
                aliasFound = false;
                tableEntryResult.addRenderResult(node.beautify(myContext, tableEntryResult, config), formatContext);
            } else if (node.is(ScanResultType.IDENTIFIER) && KEY_WORDS.contains(node.toString().toUpperCase())) {
                tableEntryResult.addRenderResult(node.beautify(myContext, tableEntryResult, config), formatContext);
            } else if (node.is(ScanResultType.IDENTIFIER) && passedANonKeyword && !aliasFound) {
                if (RelativePositionTypeEnum.AT_HORIZONTAL_POSITION
                        .equals(fromConfig.getAliasAlignment().getAlignment())) {
                    tableEntryResult.positionAt(aliasPosition);
                } else {
                    aliases.put(node, node);
                    int pos = tableEntryResult.getPosition();
                    if (pos > aliasPosition && pos <= fromConfig.getAliasAlignment().getMaxPosition().intValue()) {
                        aliasPosition = pos;
                    }
                }
                aliasFound = true;
                tableEntryResult.addRenderResult(node.beautify(myContext, tableEntryResult, config), formatContext);
            } else {
                if (node.getType().isInterpretable()) {
                    passedANonKeyword = true;
                }
                tableEntryResult.addRenderResult(node.beautify(myContext, tableEntryResult, config), formatContext);
            }
        }
        if (RelativePositionTypeEnum.AT_HORIZONTAL_POSITION.equals(fromConfig.getAliasAlignment().getAlignment())
                || aliases.isEmpty()) {
            result.addRenderResult(tableEntryResult, formatContext);
            if (log.isTraceEnabled()) {
                log.trace(new StringBuilder().append("beautify no or fixed alias positioning: ").append(this)
                        .append("\nconfig: ").append(Util.xmlBeanToString(fromConfig)).append("=\n")
                        .append(result.beautify()));
            }
            return result;
        }

        /*
         * The alias position must be vertically aligned here. And now we know the alias position
         */
        result = new RenderMultiLines(null, formatContext).setIndent(decideOnIndent(config, containsComma));
        tableEntryResult = new RenderMultiLines(null, formatContext).setIndent(0);
        for (ScanResult node = firstNonWhitespace; node != null; node = node.getNext()) {
            if (node.is(ScanResultType.CHARACTER) && ",".equals(node.toString())) {
                tableEntryResult.removeTrailingSpaces();
                result.addRenderResult(tableEntryResult, formatContext);
                nextElementOnNextLine(result, fromConfig.getComma(), formatContext);
                tableEntryResult = new RenderMultiLines(null, formatContext);
            } else if (node.is(ScanResultType.IDENTIFIER) && JOIN_WORDS.contains(node.toString().toUpperCase())) {
                tableEntryResult.removeTrailingSpaces();
                result.addRenderResult(tableEntryResult, formatContext);
                result.addLine();
                tableEntryResult = new RenderMultiLines(null, formatContext);
                for (; (node.is(ScanResultType.IDENTIFIER) && JOIN_WORDS.contains(node.toString().toUpperCase()))
                        || !node.getType().isInterpretable(); node = node.getNext()) {
                    tableEntryResult.addRenderResult(node.beautify(myContext, tableEntryResult, config), formatContext);
                }
                tableEntryResult.addRenderResult(node.beautify(myContext, tableEntryResult, config), formatContext);
            } else if (aliases.containsKey(node)) {
                tableEntryResult.positionAt(aliasPosition);
                tableEntryResult.addRenderResult(node.beautify(myContext, tableEntryResult, config), formatContext);
            } else {
                tableEntryResult.addRenderResult(node.beautify(myContext, tableEntryResult, config), formatContext);
            }
        }
        result.addRenderResult(tableEntryResult, formatContext);

        if (log.isTraceEnabled()) {
            log.trace(new StringBuilder().append("beautify vertical alias positioning: ").append(this)
                    .append("\nconfig: ").append(Util.xmlBeanToString(fromConfig)).append("=\n")
                    .append(result.beautify()));
        }
        return result;
    }

    /**
     * Returns the number of spaces that the from clause is to be indented
     *
     * @param config
     *            The FormatConfiguration that specifies if FROM is on a separate line and where commas are to be placed
     * @param containsComma
     *            Indicates if there is any comma in the table list
     * @return int The indent size
     */
    private static int decideOnIndent(FormatConfiguration config, boolean containsComma) {
        if (config.getQueryConfig().isMajorKeywordsOnSeparateLine().booleanValue()) {
            if (containsComma && BeforeOrAfterType.BEFORE.equals(config.getFromItemGrouping().getComma())) {
                return FormatContext.indent(true).length() - 2;
            }
            return FormatContext.indent(true).length();
        } else {
            if (containsComma && BeforeOrAfterType.BEFORE.equals(config.getFromItemGrouping().getComma())) {
                return 3;
            }
            return 5;
        }
    }

    /**
     * Places a comma at the end of the current line and adds a line feed or adds a line feed and a comma on the next
     * line, depending on the commaBeforeOrAfter setting of the commaSeparatedListGrouping
     *
     * @param result
     *            The render result that needs a new line and a comma
     * @param commaBeforeOrAfter
     *            {@link BeforeOrAfterType} that tells where to put the comma
     * @param formatContext
     *            for rendering
     */
    private static void nextElementOnNextLine(RenderMultiLines result, BeforeOrAfterType commaBeforeOrAfter,
            FormatContext formatContext) {
        if (BeforeOrAfterType.BEFORE.equals(commaBeforeOrAfter)) {
            int indent = result.getStandardIndent();
            result.setIndent(indent - 2);
            result.addLine();
            result.setIndent(indent);
            result.addRenderResult(new RenderItem(",", RenderItemType.CHARACTER), formatContext);
            result.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
        } else {
            result.addRenderResult(new RenderItem(",", RenderItemType.CHARACTER), formatContext);
            result.addLine();
        }
    }
}
