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

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CaseThenPositionOption;
import com.splendiddata.pgcode.formatter.internal.CaseFormatContext;
import com.splendiddata.pgcode.formatter.internal.CaseFormatContext.RenderPhase;
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
        assert "begin".equalsIgnoreCase(scanResult
                .toString()) : "A PlpgsqlBeginEndBlock should start with the word BEGIN, not with " + scanResult;
        int beginEndLevel = scanResult.getBeginEndLevel();
        ScanResult currentNode;
        ScanResult priorNode = getStartScanResult();
        for (currentNode = priorNode.getNext(); currentNode != null
                && (currentNode.getBeginEndLevel() > beginEndLevel || (currentNode.getBeginEndLevel() == beginEndLevel
                        && !"exception".equalsIgnoreCase(currentNode.toString()))); currentNode = (priorNode == null
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
                if ("when".equalsIgnoreCase(currentNode.toString())) {
                    currentNode = new WhenClauseNode(currentNode,
                            (ScanResult node) -> PostgresInputReader.interpretPlpgsqlStatementStart(node), true);
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
     * @see SrcNode#getBeginEndLevel()
     *
     * @return int the beginEndLevel AFTER this object
     */
    @Override
    public int getBeginEndLevel() {
        /*
         * BEGIN increments the nesting level, but END decrements it. getBeginEndLevel() should return
         * the level AFTER the node, so should return that of the start node minus 1.
         */
        return super.getBeginEndLevel() - 1;
    }

    /**
     * @see ScanResult#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        RenderMultiLines result = getCachedRenderResult(formatContext, parentResult, config);
        if (result != null) {
            return result;
        }

        int parentPosition = 0;
        if (parentResult != null) {
            if (!parentResult.isLastNonWhiteSpaceEqualToLinefeed()) {
                parentResult.addLine();
            }
            parentPosition = parentResult.getPosition();
        }
        ScanResult node = getStartScanResult(); // begin
        int beginEndLevel = node.getBeginEndLevel();
        FormatContext contentContext = new FormatContext(config, formatContext)
                .setAvailableWidth(formatContext.getAvailableWidth() - config.getStandardIndent());
        result = new RenderMultiLines(this, contentContext, parentResult).setIndentBase(parentPosition)
                .setIndent(config.getStandardIndent());

        /*
         * The word begin and a line feed
         */
        result.addRenderResult(node.beautify(contentContext, result, config), contentContext);
        result.addLine();
        for (node = node.getNextNonWhitespace(); node != null
                && node.getBeginEndLevel() >= beginEndLevel; node = node.getNext()) {
            if (node.is(ScanResultType.IDENTIFIER) && "exception".equalsIgnoreCase(node.toString())) {
                result.positionAfterLastNonWhitespace();
                result.setIndent(0);
                result.addLine();
                result.addRenderResult(node.beautify(contentContext, result, config), contentContext);
                result.setIndent(config.getStandardIndent());
                contentContext = new CaseFormatContext(config, contentContext, config.getCaseWhen());
                if (CaseThenPositionOption.THEN_AFTER_WHEN_ALIGNED
                        .equals(config.getCaseWhen().getThenPosition().getValue())) {
                    ((CaseFormatContext) contentContext).setRenderPhase(RenderPhase.DETERMINE_THEN_POSITION);
                    for (ScanResult whenStatement = node
                            .getNextInterpretable(); whenStatement instanceof WhenClauseNode; whenStatement = whenStatement
                                    .getNextInterpretable()) {
                        whenStatement.beautify(contentContext, result, config);
                    }
                    ((CaseFormatContext) contentContext).setRenderPhase(RenderPhase.RENDER_NORMAL);
                }
            } else {
                if (node.is(ScanResultType.LINEFEED)) {
                    result.addLine();
                } else {
                    if (!result.isLastNonWhiteSpaceEqualToLinefeed() && node.getType().isInterpretable()) {
                        result.addLine();
                    }
                    result.addRenderResult(node.beautify(contentContext, result, config), contentContext);
                }
            }
        }
        result.positionAfterLastNonWhitespace();
        result.setIndent(0).addLine();
        for (; node != null; node = node.getNext()) {
            result.addRenderResult(node.beautify(contentContext, result, config), formatContext);
        }
        return cacheRenderResult(result, formatContext, parentResult);
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

    /**
     * @see ScanResult#getSingleLineWidth(FormatConfiguration)
     *
     * @return -1 as a begin ... end block never renders to a single line
     */
    @Override
    public int getSingleLineWidth(FormatConfiguration config) {
        return -1;
    }

}
