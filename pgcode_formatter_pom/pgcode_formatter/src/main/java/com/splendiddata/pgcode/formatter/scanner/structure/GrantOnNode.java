/*
 * Copyright (c) Splendid Data Product Development B.V. 2020 - 2021
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
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Grant command. The ON roles part
 */
public class GrantOnNode extends SrcNode {
    private final List<SrcNode> constituentParts = new LinkedList<>();
    private final LinkedList<LinkedList<SrcNode>> arguments = new LinkedList<>();
    private SrcNode onClause;

    /**
     * Constructor.
     * 
     * @param scanResult
     *            The node that contain "ON" in GRANT on Roles
     */
    public GrantOnNode(ScanResult scanResult) {
        super(ScanResultType.GRANT_ON, scanResult);
        identifyTargetListElements(scanResult);
    }

    /**
     * Identifies the constituent parts of the roles in GRANT on Roles
     */
    private void identifyTargetListElements(ScanResult argumentsStart) {
        LinkedList<SrcNode> argument = new LinkedList<>();
        arguments.add(argument);

        ScanResult node = argumentsStart;
        ScanResult previousNode = node;

        if ("on".equalsIgnoreCase(node.getText().toLowerCase())) {
            onClause = new IdentifierNode(node);
            onClause.setNext(null);
            node = node.getNext();
        } else {
            return;
        }

        for (;;) {
            SrcNode argumentPart = null;
            switch (node.getType()) {
            case CHARACTER:
                switch (node.getText()) {
                case ",":
                    constituentParts.add(new CharacterNode(node));
                    argument = new LinkedList<>();
                    arguments.add(argument);
                    previousNode = node;
                    node = node.getNext();
                    continue;
                case "(":
                    constituentParts.add(new CharacterNode(node));
                    argument.add(new CharacterNode(node));

                    ScanResult startNode = node.getNext();
                    ScanResult prevNode = node;
                    ScanResult endNode = startNode;
                    while (getStartScanResult().getParenthesisLevel() < node.getParenthesisLevel()) {
                        prevNode = node;
                        node = node.getNext();
                        endNode = endNode.getNext();
                    }
                    prevNode.setNext(null);

                    while (startNode != null) {
                        argumentPart = PostgresInputReader.interpretStatementBody(startNode);
                        argument.add(argumentPart);
                        constituentParts.add(argumentPart);

                        startNode = argumentPart.getNext();
                    }
                    argument.add(new CharacterNode(node));

                    constituentParts.add(new CharacterNode(node));
                    node = node.getNext();
                    continue;
                case ")":
                    if (getStartScanResult().getParenthesisLevel() == node.getParenthesisLevel()) {
                        constituentParts.add(new CharacterNode(node));
                        setNext(node.getNext());
                        return;
                    } else if (node.getParenthesisLevel() < getStartScanResult().getParenthesisLevel()) {
                        setNext(node);
                        previousNode.setNext(null);
                        return;
                    } else {
                        argumentPart = new CharacterNode(node);
                    }
                    break;
                default:
                    argumentPart = PostgresInputReader.interpretStatementBody(node);
                }
                break;
            case COMMENT:
            case COMMENT_LINE:
            case WORD:
                argumentPart = new WordNode(node);
                break;
            case DOUBLE_QUOTED_IDENTIFIER:
            case IDENTIFIER:

                // TO, WITH
                switch (node.getText().toLowerCase()) {
                case "to":
                case "with":
                    previousNode.setNext(null);
                    setNext(node);

                    return;
                default:
                    argumentPart = PostgresInputReader.interpretIdentifier(node);
                    break;
                }
                break;
            case EOF:
                previousNode.setNext(null);
                setNext(node);
                return;
            case LITERAL:
                argumentPart = new LiteralNode(node);
                break;
            case ESCAPE_STRING:
                argumentPart = new EscapeStringNode(node);
                break;
            case WHITESPACE:
                argumentPart = new WhitespaceNode(node);
                break;
            case LINEFEED:
                argumentPart = new LinefeedNode(node);
                break;
            case SEMI_COLON:
                previousNode.setNext(null);
                setNext(node);
                return;
            case ERROR:
                return;
            default:
                throw new AssertionError("Unexpected ScanResultType." + node.getType());
            }

            assert argumentPart != null : "The namePart must not be null here";

            constituentParts.add(argumentPart);
            argument.add(argumentPart);
            previousNode = node;
            node = argumentPart.getNext();
            if (node == null) {
                previousNode.setNext(null);
                setNext(null);
                return;
            }

        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (onClause != null) {
            result.append(onClause.toString());
        }

        for (SrcNode srcNode : constituentParts) {
            result.append(srcNode.toString());

        }

        return result.toString();
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult, FormatConfiguration config) {
        RenderMultiLines result = new RenderMultiLines(this, formatContext, parentResult);

        RenderResult renderResult = onClause.beautify(formatContext, result, config);
        result.addRenderResult(renderResult, formatContext);

        for (SrcNode srcNode : constituentParts) {
            renderResult = srcNode.beautify(formatContext, result, config);
            result.addRenderResult(renderResult, formatContext);
        }

        return result;
    }
}