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
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Create type class. There are different way to create type in postgres. Examples:
 * <p>
 * CREATE TYPE name AS ...
 * <p>
 * CREATE TYPE name (...)
 * <p>
 * CREATE TYPE name;
 */
public class CreateTypeNode extends SrcNode {

    IdentifierNode specifiedTypeName;

    /**
     * Constructor.
     * 
     * @param startNode
     *            The start node of create type statement, i.e. the node that contains "create".
     */
    public CreateTypeNode(ScanResult startNode) {
        super(ScanResultType.FUNCTION_DEFINITION, startNode);

        ScanResult node = startNode;
        ScanResult priorNode = node;

        if ("create".equalsIgnoreCase(startNode.getText())) {
            node = startNode.getNextInterpretable();
            if (node != null) {
                if ("type".equalsIgnoreCase(node.getText())) {
                    priorNode = node;
                    node = node.getNext();
                }
            }
        }

        while (!node.getType().isInterpretable() && !node.isEof()) {
            priorNode = node;
            node = node.getNext();
        }
        if (node.isEof()) {
            setNext(node);
            return;
        }

        specifiedTypeName = PostgresInputReader.toIdentifier(node).setNotKeyword(true);
        priorNode.setNext(specifiedTypeName);
        priorNode = specifiedTypeName;
        node = specifiedTypeName.getNext();

        ScanResult currentNode = specifiedTypeName;
        ScanResult lastInterpreted = specifiedTypeName;
        for (priorNode = lastInterpreted.locatePriorToNextInterpretable();; priorNode = lastInterpreted
                .locatePriorToNextInterpretable()) {
            currentNode = priorNode.getNext();
            if (currentNode == null || currentNode.isStatementEnd()) {
                break;
            }
            lastInterpreted = PostgresInputReader.interpretStatementBody(currentNode);
            priorNode.setNext(lastInterpreted);
        }

        if (currentNode != null && currentNode.is(ScanResultType.SEMI_COLON)) {
            lastInterpreted = new SemiColonNode(currentNode);
            priorNode.setNext(lastInterpreted);
        }

        setNext(lastInterpreted.getNext());
        lastInterpreted.setNext(null);

    }

    /**
     * @see SrcNode#toString()
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        ScanResult current = getStartScanResult();
        while (current != null && !current.isEof()) {
            result.append(current.toString());
            current = current.getNext();

        }

        return result.toString();
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        RenderMultiLines result = new RenderMultiLines(this, formatContext, parentResult);

        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
            if (node == specifiedTypeName && config.getQueryConfig().isMajorKeywordsOnSeparateLine().booleanValue()) {
                parentResult.addLine();
            }
        }
        return result;
    }
}
