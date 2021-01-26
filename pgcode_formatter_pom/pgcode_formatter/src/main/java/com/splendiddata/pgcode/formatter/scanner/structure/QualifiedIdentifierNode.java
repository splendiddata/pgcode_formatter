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

import java.util.LinkedList;
import java.util.List;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A qualified name with all the whitespace still in it
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class QualifiedIdentifierNode extends IdentifierNode {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param startNode
     *            The identifier node that starts this identifier
     */
    public QualifiedIdentifierNode(IdentifierNode startNode) {
        super();
        replaceStartScanResult(startNode);
        ScanResult lastAdded = startNode;
        ScanResult priorNode;
        ScanResult currentNode;
        for (priorNode = lastAdded.locatePriorToNextInterpretable();; priorNode = lastAdded
                .locatePriorToNextInterpretable()) {
            currentNode = priorNode.getNext();
            if (currentNode == null || !currentNode.is(ScanResultType.CHARACTER)
                    || !".".equals(currentNode.toString())) {
                break;
            }
            priorNode = currentNode.locatePriorToNextInterpretable();
            currentNode = priorNode.getNext();
            if (currentNode == null) {
                break;
            }
            if (currentNode.is(ScanResultType.IDENTIFIER)) {
                if (currentNode instanceof IdentifierNode) {
                    lastAdded = currentNode;
                } else {
                    lastAdded = new IdentifierNode(currentNode);
                }
                priorNode.setNext(lastAdded);
            } else if (currentNode.is(ScanResultType.DOUBLE_QUOTED_IDENTIFIER)) {
                if (currentNode instanceof DoubleQuotedIdentifierNode) {
                    lastAdded = currentNode;
                } else {
                    lastAdded = new DoubleQuotedIdentifierNode(currentNode);
                }
                priorNode.setNext(lastAdded);
            } else {
                break;
            }
        }
        setNext(lastAdded.getNext());
        lastAdded.setNext(null);
    }

    /**
     * Returns the qualified name with period separated name parts unquoted.
     *
     * @see com.splendiddata.pgcode.formatter.scanner.structure.IdentifierNode#getIdentifier()
     *
     * @return String qualified name with period separated name nodes
     */
    @Override
    public String getIdentifier() {
        StringBuilder result = new StringBuilder();
        String separator = "";
        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            switch (node.getType()) {
            case IDENTIFIER:
                result.append(separator).append(((IdentifierNode) node).getIdentifier());
                separator = ".";
                break;
            default:
                break;
            }
        }
        return result.toString();
    }

    /**
     * Returns the constituents of a qualified name including the period and non interpretable tokens like comment and spaces.
     *
     *
     * @return List Constituents of a qualified name
     */
    public List<ScanResult> getAllNameParts() {
        LinkedList<ScanResult> nameParts = new LinkedList();
        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
                nameParts.add(node);
        }
        return nameParts;
    }

    /**
     * @see IdentifierNode#toString()
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            result.append(node);
        }
        return result.toString();
    }

    /**
     * @see IdentifierNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult, FormatConfiguration config) {
        RenderMultiLines result = new RenderMultiLines(this, formatContext);
        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            switch (node.getType()) {
            case WHITESPACE:
            case LINEFEED:
                break;
            case COMMENT:
                result.addWhiteSpace();
                result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
                result.addWhiteSpace();
                break;
            case COMMENT_LINE:
                result.addWhiteSpace();
                //$FALL-THROUGH$
            default:
                result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
            }
        }

        return result;
    }
}
