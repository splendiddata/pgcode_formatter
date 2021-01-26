/*
 * Copyright (c) Splendid Data Product Development B.V. 2020
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

import com.splendiddata.pgcode.formatter.CodeFormatterThreadLocal;
import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Base class for plsql functions and procedures
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class FunctionBodyNode extends SrcNode {

    /**
     * Constructor
     *
     * @param firstNode
     *            the node that contains "begin"
     */
    public FunctionBodyNode(ScanResult firstNode) {
        super(ScanResultType.FUNCTION_BODY, firstNode);
        ScanResult bodyStart = firstNode;
        int beginEndLevel = bodyStart.getBeginEndLevel();
        ScanResult prior = bodyStart;
        ScanResult nextNode = bodyStart.getNext();
        SrcNode srcNode;
        boolean statementStart = true;
        while (nextNode.getBeginEndLevel() >= beginEndLevel && !nextNode.isEof()) {
            if (nextNode.getText().equals(CodeFormatterThreadLocal.getStatementEnd())) {
                setNext(nextNode);
                prior.setNext(null);
                return;
            }
            switch (nextNode.getType()) {
            case COMMENT:
                CommentNode commentNode = new CommentNode(nextNode);
                prior.setNext(commentNode);
                prior = commentNode;
                nextNode = commentNode.getNext();
                break;
            case COMMENT_LINE:
                CommentLineNode commentLineNode = new CommentLineNode(nextNode);
                prior.setNext(commentLineNode);
                prior = commentLineNode;
                nextNode = commentLineNode.getNext();
                break;
            case WHITESPACE:
                WhitespaceNode whitespaceNode = new WhitespaceNode(nextNode);
                prior.setNext(whitespaceNode);
                prior = whitespaceNode;
                nextNode = whitespaceNode.getNext();
                break;
            case IDENTIFIER:
                switch (nextNode.getText().toLowerCase()) {
                case "open":
                    OpenCursorNode openCursorNode = new OpenCursorNode(nextNode);
                    prior.setNext(openCursorNode);
                    prior = openCursorNode;
                    nextNode = openCursorNode.getNext();
                    statementStart = true;
                    break;
                case "for":
                    if (statementStart) {
                        srcNode = PostgresInputReader.interpretStatementStart(nextNode);
                        prior.setNext(srcNode);
                        prior = srcNode;
                        nextNode = srcNode.getNext();
                    } else {
                        IdentifierNode identifierNode = new IdentifierNode(nextNode);
                        prior.setNext(identifierNode);
                        prior = identifierNode;
                        nextNode = identifierNode.getNext();
                    }
                    break;
                case "in":
                    IdentifierNode identifierNode = new IdentifierNode(nextNode);
                    prior.setNext(identifierNode);
                    prior = identifierNode;
                    nextNode = identifierNode.getNext();
                    statementStart = false;
                    break;
                case "begin":
                    identifierNode = new IdentifierNode(nextNode);
                    prior.setNext(identifierNode);
                    prior = identifierNode;
                    nextNode = identifierNode.getNext();
                    statementStart = true;
                    break;
                default:
                    if (statementStart) {
                        srcNode = PostgresInputReader.interpretStatementStart(nextNode);
                    } else {
                        srcNode = PostgresInputReader.interpretStatementBody(nextNode);
                    }
                    prior.setNext(srcNode);
                    prior = srcNode;
                    nextNode = srcNode.getNext();
                    statementStart = false;
                    break;
                }
                break;
            case SEMI_COLON:
                srcNode = new SemiColonNode(nextNode);
                prior.setNext(srcNode);
                prior = srcNode;
                nextNode = srcNode.getNext();
                statementStart = true;
                break;
            case LINEFEED:
                LinefeedNode linefeedNode = new LinefeedNode(nextNode);
                prior.setNext(linefeedNode);
                prior = linefeedNode;
                nextNode = linefeedNode.getNext();
                break;
            default:
                if (statementStart) {
                    srcNode = PostgresInputReader.interpretStatementStart(nextNode);
                } else {
                    srcNode = PostgresInputReader.interpretStatementBody(nextNode);
                }
                prior.setNext(srcNode);
                prior = srcNode;
                nextNode = srcNode.getNext();
                break;
            }
        }

        setNext(nextNode.getNext());
        nextNode.setNext(null);
    }

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

    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        RenderMultiLines result = new RenderMultiLines(this, formatContext);

        ScanResult current = getStartScanResult();
        while (current != null && !current.isEof()) {
            if ("end".equalsIgnoreCase(current.getText())) {
                result.addLine();
            }
            result.addRenderResult(current.beautify(formatContext, result, config), formatContext);
            if ("begin".equalsIgnoreCase(current.getText())) {
                if (current.getNextNonWhitespace() != null) {
                    if (ScanResultType.SEMI_COLON.equals(current.getNextNonWhitespace().getType())) {
                        current = current.getNextNonWhitespace();
                        result.addRenderResult(current.beautify(formatContext, result, config), formatContext);
                        current = current.getNext();
                    }
                    result.addLine();
                }
            }
            current = current.getNext();
        }

        return result;
    }
}
