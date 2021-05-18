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
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.internal.Util;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * The WITH clause of a DML statement. For the formatter the WITH clause is implemented as if it was a statement of its
 * own, containing the actual select, insert update or delete statement.
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class WithStatement extends SrcNode {
    /**
     * Constructor
     *
     * @param startNode
     *            The word WITH that starts this statement
     */
    public WithStatement(ScanResult startNode) {
        super(ScanResultType.INTERPRETED, PostgresInputReader.toIdentifier(startNode));
        assert "with".equalsIgnoreCase(startNode.toString()) : "A WithStatement must start with the word WITH, not: "
                + startNode;
        ScanResult priorNode = getStartScanResult().locatePriorToNextInterpretable();
        ScanResult currentNode = priorNode.getNext();
        if (currentNode != null && "recursive".equalsIgnoreCase(currentNode.toString())) {
            priorNode.setNext(PostgresInputReader.toIdentifier(currentNode));
            priorNode = priorNode.getNext().locatePriorToNextInterpretable();
            currentNode = priorNode.getNext();
        }
        if (currentNode == null || currentNode.isStatementEnd()) {
            return;
        }
        currentNode = CommaSeparatedList.ofDistinctElementTypes(currentNode, node -> new WithQuery(node));
        priorNode.setNext(currentNode);
        priorNode = currentNode.locatePriorToNextInterpretable();
        currentNode = PostgresInputReader.interpretStatementStart(priorNode.getNext());
        priorNode.setNext(currentNode);
        if (currentNode != null) {
            setNext(currentNode.getNext());
            currentNode.setNext(null);
        }
    }

    /**
     * @see ScanResult#beautify(FormatContext, RenderMultiLines,
     *      FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult, FormatConfiguration config) {
        int availableWidth = formatContext.getAvailableWidth();
        RenderMultiLines result = new RenderMultiLines(this, formatContext).setIndent(0);
        ScanResult node;
        FormatContext contentContext = new FormatContext(config, formatContext);
        for (node = getStartScanResult(); node != null && result.getHeight() <= 1; node = node.getNext()) {
            result.addRenderResult(
                    node.beautify(contentContext.setAvailableWidth(availableWidth - result.getPosition()), result, config),
                    formatContext);
        }
        /*
         * straight forward rendering
         */
        if (result.getHeight() == 1 && result.getWidth() <= availableWidth) {
            return result;
        }

        /*
         * Doesn't fit, try again
         */
        String standardIndent = FormatContext.indent(true);
        result = new RenderMultiLines(this, formatContext).setIndent(0);
        for (node = getStartScanResult(); node != null
                && !(node instanceof CommaSeparatedList); node = node.getNext()) {
            result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
        }
        if (node == null) {
            // restore available width
            return result;
        }

        String withClauseStartIndent = standardIndent;
        result.removeTrailingSpaces();
        if (result.getPosition() == "with".length()) {
            /* Only a the word "with", not "recursive" */
            result.addRenderResult(new RenderItem(" ", RenderItemType.WHITESPACE), formatContext);
            withClauseStartIndent = Util.nSpaces(result.getPosition());
        } else {
            withClauseStartIndent = standardIndent;
            result.addLine(withClauseStartIndent);
        }
        result.setIndent(withClauseStartIndent);
        result.addRenderResult(
                node.beautify(contentContext.setAvailableWidth(availableWidth - withClauseStartIndent.length()), result, config),
                formatContext);
        formatContext.setAvailableWidth(availableWidth);
        result.setIndent(0);
        result.addLine();
        for (node = node.getNextNonWhitespace(); node != null; node = node.getNext()) {
            result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
        }
        return result;
    }

    /**
     * @see com.splendiddata.pgcode.formatter.scanner.structure.SrcNode#getText()
     *
     * @return String the text of this with clause
     */
    @Override
    public String getText() {
        return toString();
    }

}
