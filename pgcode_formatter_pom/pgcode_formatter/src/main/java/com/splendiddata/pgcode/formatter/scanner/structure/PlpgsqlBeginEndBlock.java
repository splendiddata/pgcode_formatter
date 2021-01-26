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

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A PLpgSqL compound statement
 * 
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class PlpgsqlBeginEndBlock extends SrcNode implements WantsNewlineBefore {

    /**
     * Constructor
     *
     * @param scanResult
     *            The word "BEGIN"
     */
    public PlpgsqlBeginEndBlock(ScanResult scanResult) {
        super(ScanResultType.PLPGSQL_BEGIN_END_BLOCK, PostgresInputReader.toIdentifier(scanResult));
        assert "begin".equalsIgnoreCase(
                scanResult.toString()) : "A PlpgsqlBeginEndBlock should start with the word BEGIN, not with "
                        + scanResult;
        int beginEndLevel = scanResult.getBeginEndLevel();
        ScanResult currentNode;
        ScanResult priorNode = getStartScanResult();
        for (currentNode = priorNode.getNext(); currentNode != null
                && (currentNode.getBeginEndLevel() >= beginEndLevel || (currentNode != null
                        && "exception".equalsIgnoreCase(currentNode.toString()))); currentNode = (priorNode == null
                                ? null
                                : priorNode.getNext())) {
            currentNode = PostgresInputReader.interpretPlpgsqlStatementStart(priorNode.getNext());
            priorNode.setNext(currentNode);
            priorNode = currentNode;
        }
        if (currentNode != null && "exception".equalsIgnoreCase(currentNode.toString())) {
            priorNode.setNext(currentNode);
            priorNode = currentNode;
            currentNode = currentNode.getNextInterpretable();
            for (currentNode = priorNode.getNext(); currentNode != null
                    && currentNode.getBeginEndLevel() >= beginEndLevel; currentNode = priorNode.getNext()) {
                if ("exception".equalsIgnoreCase(currentNode.toString())) {
                    currentNode = new WhenClauseNode(currentNode,
                            (ScanResult node) -> PostgresInputReader.interpretPlpgsqlStatementStart(node));
                } else {
                    currentNode = PostgresInputReader.interpretPlpgsqlStatementStart(priorNode.getNext());
                }
                priorNode.setNext(currentNode);
                priorNode = currentNode;
            }
        }

        if (currentNode != null && "end".equalsIgnoreCase(currentNode.toString())) {
            currentNode = PostgresInputReader.interpretStatementBody(currentNode);
            priorNode.setNext(currentNode);
            priorNode = currentNode;
            currentNode = currentNode.getNextInterpretable();
            if (currentNode == null || !currentNode.is(ScanResultType.SEMI_COLON)) {
                /*
                 * Apparently this code block does not end in an end
                 */
                currentNode = priorNode.getNext();
            } else {
                priorNode = currentNode;
                currentNode = currentNode.getNext();
            }
        }
        setNext(currentNode);
        if (priorNode != null) {
            priorNode.setNext(null);
        }
    }

    /**
     * @see ScanResult#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        ScanResult node = getStartScanResult(); // begin
        int beginEndLevel = node.getBeginEndLevel();
        int standardIndent = FormatContext.indent(true).length();

        /*
         * The word begin and a line feed
         */
        RenderMultiLines result = new RenderMultiLines(this, formatContext).setIndent(0);
        result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
        result.addLine();

        /*
         * The content of the compound statement
         */
        FormatContext contentContext = new FormatContext(config, formatContext)
                .setAvailableWidth(formatContext.getAvailableWidth() - standardIndent);
        RenderMultiLines contentResult = new RenderMultiLines(null, formatContext);
        for (node = node.getNextNonWhitespace(); node != null
                && node.getBeginEndLevel() >= beginEndLevel; node = node.getNext()) {
            if (node.is(ScanResultType.IDENTIFIER) && "exception".equalsIgnoreCase(node.toString())) {
                contentResult.removeTrailingLineFeeds();
                contentResult.removeTrailingSpaces();
                result.addRenderResult(contentResult, formatContext);
                result.addLine();
                result.addRenderResult(node.beautify(contentContext, result, config), formatContext);
                result.addLine();
                contentResult = new RenderMultiLines(null, formatContext);
            } else {
                if (node.is(ScanResultType.LINEFEED)) {
                    contentResult.addLine();
                } else {
                    if (node.getType().isInterpretable() || contentResult.isLastNonWhiteSpaceEqualToLinefeed()) {
                        contentResult.positionAt(standardIndent);
                    }
                    contentResult.addRenderResult(node.beautify(contentContext, contentResult, config), formatContext);
                }
            }
        }
        contentResult.removeTrailingLineFeeds();
        contentResult.removeTrailingSpaces();
        result.addRenderResult(contentResult, formatContext);
        result.addLine();
        for (; node != null; node = node.getNext()) {
            result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
        }
        return result;
    }

    /**
     * @see com.splendiddata.pgcode.formatter.scanner.structure.SrcNode#getText()
     *
     * @return String the entire original text of the declare section
     */
    @Override
    public String getText() {
        StringBuilder result = new StringBuilder();
        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            result.append(node);
        }
        return result.toString();
    }

    /**
     * @see java.lang.Object#toString()
     *
     * @return String the entire original text of the declare section
     */
    @Override
    public String toString() {
        return getText();
    }
}
