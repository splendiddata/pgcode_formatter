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
 * Implements a statement for which no specific implementation can be identified (yet).
 * <p>
 * A statement is supposed to start at the first identifier node after a previous statement and is supposed to end at a
 * semicolon or EOF
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class JustAStatementNode extends SrcNode {

    /**
     * Constructor
     *
     * @param scanResult
     *            The Identifier that starts the statement
     */
    public JustAStatementNode(ScanResult scanResult) {
        super(ScanResultType.JUST_A_STATEMENT,
                scanResult instanceof IdentifierNode ? scanResult : new IdentifierNode(scanResult));
        ScanResult currentNode = this.getStartScanResult();
        ScanResult lastInterpreted = currentNode;
        int parenthesesLevel = currentNode.getParenthesisLevel();
        ScanResult priorNode = currentNode;
        for (priorNode = lastInterpreted.locatePriorToNextInterpretable();; priorNode = lastInterpreted
                .locatePriorToNextInterpretable()) {
            currentNode = priorNode.getNext();
            if (currentNode == null || currentNode.getParenthesisLevel() < parenthesesLevel || currentNode.isEof()) {
                break;
            }
            currentNode = PostgresInputReader.interpretStatementBody(currentNode);
            if (currentNode.is(ScanResultType.PSQL_META_COMMAND)) {
                break;
            }
            if (ScanResultType.IDENTIFIER.equals(currentNode.getType())
                    && "begin".equalsIgnoreCase(currentNode.getText())) {
                break;
            }
            lastInterpreted = currentNode;
            priorNode.setNext(lastInterpreted);
            if (lastInterpreted.is(ScanResultType.SEMI_COLON)) {
                break;
            }
        }
        setNext(lastInterpreted.getNext());
        lastInterpreted.setNext(null);
    }

    /**
     * @see ScanResult#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        int availableWidth = formatContext.getAvailableWidth();
        String standardIndent = FormatContext.indent(true);
        ScanResult node = this.getStartScanResult();
        RenderMultiLines result = new RenderMultiLines(node, formatContext);
        for (; node != null; node = node.getNext()) {
            RenderResult intermediate = node
                    .beautify(formatContext.setAvailableWidth(availableWidth - result.getPosition()), result, config);
            if (result.getPosition() > standardIndent.length() && (intermediate.getHeight() > 1
                    || result.getPosition() + intermediate.getWidth() > availableWidth)) {
                RenderResult attempt2 = node.beautify(
                        formatContext.setAvailableWidth(availableWidth - standardIndent.length()), result, config);
                if (attempt2.getHeight() < intermediate.getHeight()
                        || result.getPosition() + intermediate.getWidth() > availableWidth) {
                    result.addLine();
                    result.addRenderResult(attempt2, formatContext);
                } else {
                    result.addRenderResult(intermediate, formatContext);
                }
            } else {
                result.addRenderResult(intermediate, formatContext);
            }
        }
        formatContext.setAvailableWidth(availableWidth); // restore
        return result;
    }

    /**
     * @see com.splendiddata.pgcode.formatter.scanner.structure.SrcNode#getText()
     *
     * @return String the text of the statement
     */
    @Override
    public String getText() {
        return toString();
    }

    /**
     * @see java.lang.Object#toString()
     *
     * @return String the text of the statement
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            result.append(node);
        }
        return result.toString();
    }

}
