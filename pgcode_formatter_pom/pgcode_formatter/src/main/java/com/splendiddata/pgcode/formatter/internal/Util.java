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

package com.splendiddata.pgcode.formatter.internal;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.xml.bind.JAXB;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.*;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.TabsOrSpacesType;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultStringLiteral;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;
import com.splendiddata.pgcode.formatter.scanner.structure.*;

/**
 * Some utility functions
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public final class Util {
    /**
     * The linefeed character(s) for the operating system
     */
    public static final String LF = System.lineSeparator();

    /**
     * A pattern containing just a linefeed. Might be usefull splitting lines on linefeed characters.
     */
    public static final Pattern NEWLINE_PATTERN = Pattern.compile("\\n");

    private static final Logger log = LogManager.getLogger(Util.class);
    /**
     * Cache of strings with a specified number of spaces
     */
    private static final Map<Integer, String> spacesCache = new HashMap<>();
    /**
     * Cache of strings with a specified number of tabs
     */
    private static final Map<Integer, String> tabsCache = new HashMap<>();
    public static String space = " ";

    /**
     * Utility class - no instances
     *
     * @throws UnsupportedOperationException
     *             in all cases
     */
    private Util() {
        throw new UnsupportedOperationException(getClass().getName() + " is a utility class - no instances");
    }

    /**
     * Returns a (cached) String with n spaces
     *
     * @param n
     *            The number of space characters desired
     * @return String with n spaces
     */
    public static String nSpaces(int n) {
        if (n <= 0) {
            return "";
        }
        String result = spacesCache.get(Integer.valueOf(n));
        if (result == null) {
            byte[] bytes = new byte[n];
            Arrays.fill(bytes, (byte) ' ');
            result = new String(bytes);
            spacesCache.put(Integer.valueOf(n), result);
        }
        return result;
    }

    /**
     * Returns a (cached) String with n tabs
     *
     * @param n
     *            The number of tab characters desired
     * @return String with n tab charactes
     */
    public static String nTabs(int n) {
        String result = tabsCache.get(Integer.valueOf(n));
        if (result == null) {
            byte[] bytes = new byte[n];
            Arrays.fill(bytes, (byte) '\t');
            result = new String(bytes);
            tabsCache.put(Integer.valueOf(n), result);
        }
        return result;
    }

    /**
     * Null-safe check if the specified collection is empty.
     * 
     * @param collection
     *            the collection to check, may be null
     * @return true if null or empty
     */
    public static boolean isNullOrEmpty(Collection<?> collection) {
        return (collection == null || collection.isEmpty());
    }

    public static RenderItemType convertScanResultTypeToRenderItemType(ScanResultType scanResultType) {
        switch (scanResultType) {
        case WHITESPACE:
            return RenderItemType.WHITESPACE;
        case IDENTIFIER:
            return RenderItemType.IDENTIFIER;
        case DOUBLE_QUOTED_IDENTIFIER:
            return RenderItemType.DOUBLE_QUOTED_IDENTIFIER;
        case LITERAL:
            return RenderItemType.LITERAL;
        case ESCAPE_STRING:
            return RenderItemType.ESCAPE_STRING;
        case WORD:
            return RenderItemType.IDENTIFIER;
        case CHARACTER:
            return RenderItemType.CHARACTER;
        case COMMENT:
            return RenderItemType.COMMENT;
        case COMMENT_LINE:
            return RenderItemType.COMMENT_LINE;
        case SEMI_COLON:
            return RenderItemType.SEMI_COLON;
        case LINEFEED:
            return RenderItemType.LINEFEED;
        case COMBINED_IDENTIFIER:
            return RenderItemType.COMBINED_IDENTIFIER;
        case FUNCTION_CALL:
        case EOF:
        case ERROR:
        case TEXT:
        case TYPE_DECLARATION:
        case DATA_TYPE:
        case DATA_DECLARATION:
        case FUNCTION_SIGNATURE:
        case FUNCTION_DEFINITION:
        case FUNCTION_BODY:
        case CASE_CLAUSE:
        case CASE_STATEMENT:
        case WHEN_THEN_CLAUSE:
        case SELECT_STATEMENT:
        case DISCOVERED_DATA_TYPE:
        case VALUE_LIST:
        default:
            return null;
        }
    }

    /**
     * Starting with the fromScanResult, this method walks the {@link ScanResult#getNext()} list until it returns null,
     * and renders every node subsequently into the renderResult. When a rendered line gets too long, it will be broken
     * and rendering will continue on the next line.
     *
     * @param fromScanResult
     *            The ScanResult that is to be rendered first.
     * @param renderResult
     *            The RenderMultiLines to which the scan results will be rendered
     * @param formatContext
     *            The FormatContext to use while rendering
     * @param config
     *            The FormatConfiguration that provides settings about the rendering process
     * @return the renderResult argument, appended with the rendered scan results
     */
    public static RenderMultiLines renderStraightForward(ScanResult fromScanResult, RenderMultiLines renderResult,
            FormatContext formatContext, FormatConfiguration config) {
        if (log.isTraceEnabled()) {
            log.trace("renderStraightForward invoked from " + Thread.currentThread().getStackTrace()[2]);
        }
        int availableWidth = formatContext.getAvailableWidth();
        int standardIndent = renderResult.getLocalIndent();
        FormatContext itemContext = new FormatContext(config, formatContext)
                .setAvailableWidth(availableWidth - standardIndent);
        for (ScanResult srcNode = fromScanResult; srcNode != null; srcNode = srcNode.getNext()) {
            int itemWidth = srcNode.getSingleLineWidth(config);
            int pos = renderResult.getPosition();
            if (pos > standardIndent && itemWidth >= 0 && pos + itemWidth > config.getLineWidth().getValue()
                    && !(srcNode instanceof InParentheses || srcNode instanceof CommaSeparatedList)) {
                renderResult.addLine();
            }
            renderResult.addRenderResult(srcNode.beautify(itemContext, renderResult, config), formatContext);
        }
        return renderResult;
    }

    /**
     * Invokes (@link ScanResult#getSingleLineWidth(FormatConfiguration)} on fromScanResult, and probably its following
     * nodes, to determine the total line length if rendered into a single line. As soon as the first ScanResult returns
     * a negative number, a negative number is returned to indicate that rendering on a single line will not succeed.
     *
     * @param fromScanResult
     *            The ScanResul to start with in determining the total expected line length
     * @param config
     *            May be used to get a max linelength
     * @return The total expected linelength or a negative number if rendering will need more than one line.
     */
    public static int getSingleLineWidth(ScanResult fromScanResult, FormatConfiguration config) {
        if (log.isTraceEnabled()) {
            log.trace("getSingleLineWidth invoked from " + Thread.currentThread().getStackTrace()[2]);
        }
        int expectedWidth = 0;
        int additionalWidth;
        for (ScanResult srcNode = fromScanResult; srcNode != null; srcNode = srcNode.getNext()) {
            additionalWidth = srcNode.getSingleLineWidth(config);
            if (additionalWidth < 0) {
                expectedWidth = -1;
                break;
            }
            expectedWidth += additionalWidth;
        }
        return expectedWidth;
    }

    /**
     * Marshals an xmlBean to Sting for debugging purposes
     *
     * @param xmlBean
     *            The config element to display
     * @return String
     */
    public static String xmlBeanToString(Object xmlBean) {
        StringWriter w = new StringWriter();
        JAXB.marshal(xmlBean, w);
        return w.toString().replaceAll("<\\?.*\\?>\n", "");
    }

    public static SrcNode interpretStatement(ScanResult startNode) {

        if (startNode instanceof SrcNode) {
            return (SrcNode) startNode;
        }

        switch (startNode.getType()) {
        case CHARACTER:
        case OPENING_PARENTHESIS:
        case CLOSING_PARENTHESIS:
            return new CharacterNode(startNode);
        case COMMENT:
            return new CommentNode(startNode);
        case COMMENT_LINE:
            return new CommentLineNode(startNode);
        case DOUBLE_QUOTED_IDENTIFIER:
            return new IdentifierNode(startNode);
        case EOF:
            return null;
        case ERROR:
            return new ErrorNode(startNode);
        case IDENTIFIER:
            return new IdentifierNode(startNode);
        case LINEFEED:
            return new LinefeedNode(startNode);
        case LITERAL:
            return new LiteralNode(startNode);
        case ESCAPE_STRING:
            return new EscapeStringNode(startNode);
        case SEMI_COLON:
            return new SemiColonNode(startNode);
        case WHITESPACE:
            return new WhitespaceNode(startNode);
        case WORD:
            return new WordNode(startNode);
        default:
            return null;
        }
    }

    /**
     * Turns the inFile into a stream of RenderResults. Each RenderResult typically contains one statement and ends in a
     * line feed. Thus the sql file can be beautified in a streaming way.
     *
     * @param inFile
     *            The Reader of that will provide the input
     * @param config
     *            The FormatConfiguration that will be used to render the results
     * @return Stream&lt;RenderResult&gt;
     * @throws IOException
     *             from the inputFile
     */
    public static Stream<RenderResult> toRenderResults(Reader inFile, FormatConfiguration config) throws IOException {
        return StreamSupport.stream(new Spliterator<RenderResult>() {
            /**
             * Provides the start of the next result
             * <p>
             * The {@link PostgresInputReader} only provides the first result. Subsequent results can be obtained using
             * the {@link ScanResult#getNext()} method. Thus the input file is consumed in a streaming way as well.
             */
            private ScanResult nextNode = new PostgresInputReader(inFile).getFirstResult();

            /**
             * The tryAdvance effectively provides the input for the stream, one entry at a time.
             * 
             * @see java.util.Spliterator#tryAdvance(java.util.function.Consumer)
             *
             * @param action
             *            The Consumer&lt;? super RenderResult&gt; that does whatever is specified on the stream
             * @return false at end of stream, otherwise true
             */
            @Override
            public boolean tryAdvance(Consumer<? super RenderResult> action) {
                if (nextNode == null || nextNode.isEof()) {
                    return false;
                }

                FormatContext formatContext = new FormatContext(config, null);
                RenderMultiLines result = new RenderMultiLines(null, formatContext, null);

                /*
                 * First deal with empty lines
                 */
                int emptyLineCount = 0;
                for (; nextNode != null && (nextNode.is(ScanResultType.WHITESPACE)
                        || nextNode.is(ScanResultType.LINEFEED)); nextNode = nextNode.getNext()) {
                    if (nextNode.is(ScanResultType.LINEFEED)) {
                        emptyLineCount++;
                    }
                }
                if (nextNode == null || nextNode.isEof()) {
                    return false;
                }
                if (emptyLineCount > 0) {
                    switch (config.getEmptyLine()) {
                    case PRESERVE_ALL:
                        for (int i = 0; i < emptyLineCount; i++) {
                            result.addExtraLine();
                        }
                        action.accept(result);
                        return true;
                    case PRESERVE_ONE:
                        result.addExtraLine();
                        action.accept(result);
                        return true;
                    case REMOVE:
                    default:
                        break;

                    }
                }

                /*
                 * Interpret a statement
                 */
                SrcNode statementNode = PostgresInputReader.interpretStatementStart(nextNode);
                nextNode = statementNode.getNext();
                statementNode.setNext(null); // break the list to avoid any memory problems
                CodeFormatter.log.debug("Statement=<<<%s>>>\n", statementNode);
                /*
                 * Render the statement
                 */
                result.addRenderResult(statementNode.beautify(formatContext, result, config), formatContext);
                /*
                 * Not all statements include their ending semi-colon. Make sure they do now.
                 */
                ScanResult trailingNode = nextNode;
                if (trailingNode != null
                        && (trailingNode.is(ScanResultType.WHITESPACE) || trailingNode.is(ScanResultType.LINEFEED))) {
                    trailingNode = trailingNode.getNextNonWhitespace();
                }
                if (trailingNode != null && trailingNode.is(ScanResultType.SEMI_COLON)) {
                    nextNode = trailingNode.getNext();
                    trailingNode.setNext(null);
                    result.addRenderResult(trailingNode.beautify(formatContext, result, config), formatContext);
                }

                /*
                 * Add trailing comment if any. Anyway, make sure the result will end in a line feed.
                 */
                boolean foundTrailingComment = false;
                for (trailingNode = nextNode; trailingNode != null && !trailingNode.getType().isInterpretable()
                        && !trailingNode.is(ScanResultType.LINEFEED); trailingNode = trailingNode.getNext()) {
                    foundTrailingComment |= trailingNode.is(ScanResultType.COMMENT)
                            || trailingNode.is(ScanResultType.COMMENT_LINE);
                }
                if (foundTrailingComment) {
                    for (ScanResult node = nextNode; node != trailingNode; node = node.getNext()) {
                        result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
                    }
                }
                if (trailingNode != null && trailingNode.is(ScanResultType.LINEFEED)) {
                    nextNode = trailingNode.getNext();
                } else {
                    nextNode = trailingNode;
                }

                /*
                 * Now finish the line and "publish"
                 */
                result.addLine();
                action.accept(result);

                return true;
            }

            /**
             * The input file must be consumed sequentially, so this splitterator cannot be split.
             * 
             * @see java.util.Spliterator#trySplit()
             *
             * @return null in all cases
             */
            @Override
            public Spliterator<RenderResult> trySplit() {
                return null;
            }

            /**
             * We havn't got a clue on the number of statements that might be in the input here
             * 
             * @see java.util.Spliterator#estimateSize()
             *
             * @return Long.MAX_VALUE
             */
            @Override
            public long estimateSize() {
                return Long.MAX_VALUE;
            }

            /**
             * @see java.util.Spliterator#characteristics()
             *
             * @return IMMUTABLE | NONNULL | ORDERED
             */
            @Override
            public int characteristics() {
                return IMMUTABLE | NONNULL | ORDERED;
            }

            /**
             * Returns null as sorting the input file makes no sense
             * 
             * @see java.util.Spliterator#getComparator()
             *
             * @return null
             */
            @Override
            public Comparator<? super RenderResult> getComparator() {
                return null;
            }

        }, false);
    }

    /**
     * Parses the provided String and creates a list of elements of different types (like literal, comment, ...etc)
     * 
     * @param result
     *            The String that has to be parsed
     * @return A list of SplitData elements.
     */
    public static LinkedList<SplitData> parseInput(String result) {
        Reader stringReader = new StringReader(result);

        String partString = "";
        LinkedList<SplitData> split = new LinkedList<>();
        try (FormattedInputReader postgresInputReader = new FormattedInputReader(stringReader)) {
            ScanResult nextNode = postgresInputReader.getFirstResult();
            SplitData splitDataPart = new SplitData();

            while (nextNode != null && !nextNode.isEof()) {
                partString = nextNode.toString();
                if (ScanResultType.LITERAL.equals(nextNode.getType())
                        || ScanResultType.DOUBLE_QUOTED_IDENTIFIER.equals(nextNode.getType())
                        || ScanResultType.ESCAPE_STRING.equals(nextNode.getType())
                        || ScanResultType.COMMENT_LINE.equals(nextNode.getType())
                        || ScanResultType.COMMENT.equals(nextNode.getType())) {
                    SplitData splitData;
                    if (nextNode instanceof ScanResultStringLiteral) {
                        splitData = new SplitData(convertScanResultTypeToSplitDataType(nextNode.getType()),
                                nextNode.getText(), ((ScanResultStringLiteral) nextNode).getQuoteString());
                    } else {
                        splitData = new SplitData();
                        splitData.setType(convertScanResultTypeToSplitDataType(nextNode.getType()));
                        splitData.setText(partString);
                    }
                    split.add(splitDataPart);
                    split.add(splitData);
                    splitDataPart = new SplitData();
                } else {
                    splitDataPart.appendText(partString);
                }
                nextNode = nextNode.getNext();
            }
            split.add(splitDataPart);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return split;
    }

    /**
     * Converts a {@link ScanResultType} to a {@link SplitDataType}
     * 
     * @param scanResultType
     *            A {@link ScanResultType} to convert
     * @return A {@link SplitDataType}
     */
    public static SplitDataType convertScanResultTypeToSplitDataType(ScanResultType scanResultType) {
        switch (scanResultType) {
        case DOUBLE_QUOTED_IDENTIFIER:
            return SplitDataType.DOUBLE_QUOTED_IDENTIFIER;
        case LITERAL:
            return SplitDataType.LITERAL;
        case ESCAPE_STRING:
            return SplitDataType.ESCAPE_STRING;
        case COMMENT:
            return SplitDataType.COMMENT;
        case COMMENT_LINE:
            return SplitDataType.COMMENT_LINE;
        default:
            return SplitDataType.TEXT;
        }
    }

    /**
     * Replaces groups of tab characters by spaces if the config desires so.
     * <ul>
     * <li>If configuration-&gt;tabs-&gt;tabsOrSpaces equals TABS then all groups of spaces that can be replaced by tab
     * characters will be.</li>
     * <li>Else if configuration-&gt;indent-&gt;tabsOrSpaces equals TABS then only leading spaces are replaced by
     * tabs</li>
     * <li>Otherwise the original text is returned</li>
     * </ul>
     *
     * @param config
     *            The configuration that will provide the tabs and indent setting
     * @param textWithSpaces
     *            The text of which groups of spaces may be replaced by tabs
     * @return The resulting string
     */
    public static String performTabReplacement(FormatConfiguration config, String textWithSpaces) {
        String result = textWithSpaces;
        if (TabsOrSpacesType.TABS.equals(config.getTabs().getTabsOrSpaces())) {
            result = replaceSpacesByTabs(config, result);
        } else if (TabsOrSpacesType.TABS.equals(config.getIndent().getTabsOrSpaces())) {
            result = replaceLeadingSpaces(config, result);
        }
        return result;
    }

    /**
     * Replaces spaces by tabs, based on the provided regular expression patterns, in the provided string.
     * 
     * @param config
     *            A FormatConfiguration where the tabWidth is defined.
     * @param textWithSpaces
     *            The String that has to be adapted
     * @return The adapted String
     */
    private static String replaceSpacesByTabs(FormatConfiguration config, String textWithSpaces) {
        LinkedList<SplitData> split = parseInput(textWithSpaces);
        String partString;
        Pattern tabSplitPattern = config.getTabSplitPattern();
        Pattern tabReplacementPattern = config.getTabReplacementPattern();

        StringBuilder buildResult = new StringBuilder();
        int tabWidth = config.getTabs().getTabWidth().intValue();

        for (int i = 0; i < split.size(); i++) {
            SplitData partData = split.get(i);
            partString = partData.toString();
            if (!SplitDataType.LITERAL.equals(partData.getType())
                    && !SplitDataType.DOUBLE_QUOTED_IDENTIFIER.equals(partData.getType())
                    && !SplitDataType.ESCAPE_STRING.equals(partData.getType())) {
                partString = tabSplitPattern.matcher(partString).results()
                        .map(part -> part.group().length() == tabWidth
                                ? tabReplacementPattern.matcher(part.group()).replaceAll("\t")
                                : part.group())
                        .collect(Collectors.joining());
            }

            buildResult.append(partString);
        }
        return buildResult.toString();
    }

    /**
     * Replaces the leading spaces by tabs, based on the provided regular expression pattern, in the provided string.
     * 
     * @param config
     *            A FormatConfiguration where the tabWidth is defined.
     * @param textWithLeadingSpaces
     *            The String that has to be adapted
     * @return The adapted String
     */
    private static String replaceLeadingSpaces(FormatConfiguration config, String textWithLeadingSpaces) {
        Pattern leadingSpacesPattern = config.getLeadingSpacesPattern();
        LinkedList<SplitData> split = parseInput(textWithLeadingSpaces);
        String partString;
        StringBuilder buildResult = new StringBuilder();

        for (int i = 0; i < split.size(); i++) {
            SplitData partData = split.get(i);
            partString = partData.toString();
            if (!SplitDataType.LITERAL.equals(partData.getType())
                    && !SplitDataType.DOUBLE_QUOTED_IDENTIFIER.equals(partData.getType())
                    && !SplitDataType.ESCAPE_STRING.equals(partData.getType())) {
                Matcher matcher = leadingSpacesPattern.matcher(partString);
                partString = matcher.replaceAll(matchResult -> {
                    int nrSpaces = matchResult.group().length() - 1;
                    int tabWidth = config.getTabs().getTabWidth().intValue();
                    return new StringBuilder().append("\n").append(nTabs(nrSpaces / tabWidth))
                            .append(nSpaces(nrSpaces % tabWidth)).toString();
                });
            }

            buildResult.append(partString);
        }
        return buildResult.toString();
    }
}
