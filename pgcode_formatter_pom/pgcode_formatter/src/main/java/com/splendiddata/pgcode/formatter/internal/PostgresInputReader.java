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

package com.splendiddata.pgcode.formatter.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.ConfigUtil;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;
import com.splendiddata.pgcode.formatter.scanner.SourceScanner;
import com.splendiddata.pgcode.formatter.scanner.SourceScannerImpl;
import com.splendiddata.pgcode.formatter.scanner.structure.*;

/**
 * Is the starting point for scanning the source code and contains utility methods to interpret it.
 * <p>
 * The first ScanResult can be obtained via the {@link #getFirstResult()} method directly after construction. Subsequent
 * ScanResults are returned by the {@link ScanResult#getNext()} method.
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class PostgresInputReader implements Closeable {
    private static final Logger log = LogManager.getLogger(PostgresInputReader.class);

    private static SourceScanner scanner;
    private ScanResult scanResult;

    /**
     * Constructor
     * <p>
     * The constructor starts interpreting the first node(s) of the input that it gets from the reader. Using the
     * {@link #getFirstResult()} method, the first result will be made available. Following results can be obtained via
     * the {@link ScanResult#getNext()} method on the returned result. The next result may come directly from the
     * scanner, which will invoke the reader if necessary, or it may have been read before. Thus the source can be
     * parsed in a streaming way.
     *
     * @param reader
     *            A Reader that will provide the input.
     * @throws IOException
     *             from the reader
     */
    public PostgresInputReader(Reader reader) throws IOException {
        scanner = new SourceScannerImpl(reader);
        scanResult = scanner.scan();
    }

    /**
     * Interpret the scanResult at or before the first word of a new statement
     * <p>
     * Multiple scan results, starting with the startNode, may be joined into the resulting SrcNode. The next pointer of
     * the startNode may have been altered as well as its role. So please forget about it after interpretation and use
     * the resulting SrcNode instead.
     *
     * @param startNode
     *            The ScanResult that is to be interpreted
     * @return SrcNode the interpreted node
     */
    public static SrcNode interpretStatementStart(ScanResult startNode) {
        return interpretStatementStart(startNode, node -> false);
    }

    /**
     * Interpret the scanResult at or before the first word of a new statement
     * <p>
     * Multiple scan results, starting with the startNode, may be joined into the resulting SrcNode. The next pointer of
     * the startNode may have been altered as well as its role. So please forget about it after interpretation and use
     * the resulting SrcNode instead.
     *
     * @param startNode
     *            The ScanResult that is to be interpreted
     * @param isComplete
     *            A Predicate&lt;ScanResult&gt; that indicates that the list for ScanResults has reached a certain node.
     * @return SrcNode the interpreted node
     */
    public static SrcNode interpretStatementStart(ScanResult startNode, Predicate<ScanResult> isComplete) {
        log.trace(() -> new StringBuilder().append("interpretStatementStart(").append(startNode).append(')'));
        if (startNode == null) {
            log.debug(() -> "interpretStatementStart(null) = null");
            return null;
        }
        if (startNode instanceof SrcNode) {
            log.debug(() -> new StringBuilder().append("interpretStatementStart(").append(startNode)
                    .append(") = existing: ").append(startNode.getClass().getName()));
            return (SrcNode) startNode;
        }

        String text = startNode.getText();
        SrcNode result = null;
        switch (startNode.getType()) {
        case CHARACTER:
            if ("\\".equals(startNode.getText())) {
                result = new PsqlMetaCommand(startNode);
            } else {
                result = interpretCharacter(startNode);
            }
            break;
        case COMMENT:
            result = new CommentNode(startNode);
            break;
        case COMMENT_LINE:
            result = new CommentLineNode(startNode);
            break;
        case DOUBLE_QUOTED_IDENTIFIER:
            result = new IdentifierNode(startNode);
            break;
        case EOF:
            result = null;
            break;
        case ERROR:
            result = new ErrorNode(startNode);
            break;
        case IDENTIFIER:
            switch (text.toLowerCase()) {
            case "create":
                result = interpretCreateStatement(startNode);
                break;
            case "import":
                //                        return new ErrorNode(startNode, new Msg(MsgKey.valueOf("msg.java.code.involved")));
                break;
            case "update":
                result = new UpdateTableNode(startNode, true, isComplete);
                break;
            case "with":
                result = new WithStatement(startNode);
                break;
            case "select":
                result = interpretSelectStatement(startNode);
                break;
            case "insert":
                result = new InsertStatement(startNode);
                break;
            case "grant":
                result = new GrantCommandNode(startNode);
                break;
            case "begin":
                if (startNode.getNextInterpretable() != null
                        && ScanResultType.SEMI_COLON.equals(startNode.getNextInterpretable().getType())) {
                    result = new IdentifierNode(startNode);
                    break;
                }
                result = new FunctionBodyNode(startNode);
                break;
            case "do":
                result = new AnonymousCodeBlockNode(startNode);
                break;
            case "revoke":
            case "drop":
            case "delete":
            case "alter":
            case "comment":
            case "set":
            case "fetch":
            case "execute":
                result = new JustAStatementNode(startNode);
                break;
            default:
                result = interpretIdentifier(startNode);
                break;
            }
            break;
        case LINEFEED:
            result = new LinefeedNode(startNode);
            break;
        case LITERAL:
            result = new LiteralNode(startNode);
            break;
        case ESCAPE_STRING:
            result = new EscapeStringNode(startNode);
            break;
        case SEMI_COLON:
            result = new SemiColonNode(startNode);
            break;
        case WHITESPACE:
            result = new WhitespaceNode(startNode);
            break;
        case WORD:
            result = new WordNode(startNode);
            break;
        case OPENING_PARENTHESIS:
            result = new InParentheses(startNode);
            break;
        case CLOSING_PARENTHESIS:
            result = new CharacterNode(startNode);
            break;
        default:
            throw new AssertionError(
                    "Unknown ScanResultType." + startNode.getType() + " in interpretStatementStart(" + startNode + ")");
        }

        if (log.isDebugEnabled()) {
            StringBuilder msg = new StringBuilder().append("interpretStatementStart(startNode=").append(startNode)
                    .append(") = ");
            if (result == null) {
                msg.append("null");
            } else {
                msg.append(result.getClass().getName());
            }
            if (log.isTraceEnabled()) {
                msg.append(':').append(result);
                log.trace(msg);
            } else {
                log.debug(msg);
            }
        }
        return result;
    }

    /**
     * Interpret a scan result that is positioned inside a statement
     * <p>
     * Multiple scan results, starting with the startNode, may be joined into the resulting SrcNode. The next pointer of
     * the startNode may have been altered as well as its role. So please forget about it after interpretation and use
     * the resulting SrcNode instead.
     *
     * @param node
     *            The scan result to interpret
     * @return SrcNode the interpreted node(s)
     */
    public static SrcNode interpretStatementBody(ScanResult node) {
        log.trace(() -> new StringBuilder().append("interpretStatementBody(node=").append(node).append(')'));
        if (node == null) {
            log.debug("interpretStatementBody(null) = null");
            return null;
        }
        if (node instanceof SrcNode) {
            log.debug(() -> new StringBuilder().append("interpretStatementBody(node=").append(node).append(") = ")
                    .append(node.getClass().getName()));
            return (SrcNode) node;
        }
        SrcNode result = null;
        switch (node.getType()) {
        case OPENING_PARENTHESIS:
            result = new InParentheses(node);
            break;
        case CHARACTER:
            switch (node.getText()) {
            case "\\":
                result = new PsqlMetaCommand(node);
                break;
            default:
                result = interpretCharacter(node);
                break;
            }
            break;
        case COMMENT:
            result = new CommentNode(node);
            break;
        case COMMENT_LINE:
            result = new CommentLineNode(node);
            break;
        case DOUBLE_QUOTED_IDENTIFIER:
            result = interpretIdentifier(node);
            break;
        case EOF:
            result = null;
            break;
        case ERROR:
            result = new ErrorNode(node);
            break;
        case IDENTIFIER:
            switch (node.toString().toLowerCase()) {
            case "case":
                result = new CaseClauseNode(node);
                break;
            case "update":
                result = new UpdateTableNode(node, false);
                break;
            case "select":
                result = new SelectStatement(node, nextNode -> {
                    if (nextNode.is(ScanResultType.SEMI_COLON)) {
                        return true;
                    }
                    return false;
                });
                break;
            default:
                result = interpretIdentifier(node);
                break;
            }
            break;
        case LINEFEED:
            result = new LinefeedNode(node);
            break;
        case LITERAL:
            result = new LiteralNode(node);
            break;
        case ESCAPE_STRING:
            result = new EscapeStringNode(node);
            break;
        case SEMI_COLON:
            result = new SemiColonNode(node);
            break;
        case WHITESPACE:
            result = new WhitespaceNode(node);
            break;
        case WORD:
            result = new WordNode(node);
            break;
        case CLOSING_PARENTHESIS:
            result = new CharacterNode(node);
            log.warn("Encountered an unmatched closing parenthesis " + "\"" + node.getText() + "\" "
                    + node.getErrorMessage());
            break;
        default:
            throw new AssertionError(
                    "Unknown ScanResultType." + node.getType() + " in interpretStatementBody(" + node + ")");
        }

        if (log.isDebugEnabled()) {
            StringBuilder msg = new StringBuilder().append("interpretStatementBody(startNode=").append(node)
                    .append(") = ");
            if (result == null) {
                msg.append("null");
            } else {
                msg.append(result.getClass().getName());
            }
            if (log.isTraceEnabled()) {
                msg.append(':').append(result);
                log.trace(msg);
            } else {
                log.debug(msg);
            }
        }
        return result;
    }

    /**
     * Interprets the start of a statement in PLpgSQL code
     * <p>
     * This method tries to identify the start of a PLpgSQL specific statement. If the provided node is not PLpgSQL
     * specific, then the execution will be delegated to {@link #interpretStatementStart(ScanResult)}
     *
     * @param node
     *            The node that might start a statement
     * @return SrcNode the interpreted node
     */
    public static SrcNode interpretPlpgsqlStatementStart(ScanResult node) {
        if (node == null) {
            return null;
        }
        if (node instanceof SrcNode) {
            return (SrcNode) node;
        }
        SrcNode result = null;
        if (node.is(ScanResultType.IDENTIFIER)) {
            switch (node.toString().toLowerCase()) {
            case "for":
                result = new ForLoopNode(node);
                break;
            case "while":
                result = new WhileLoopNode(node);
                break;
            case "declare":
                result = new PlpgsqlDeclareSection(node);
                break;
            case "begin":
                result = new PlpgsqlBeginEndBlock(node);
                break;
            case "if":
                result = new PlpgsqlIfStatement(node);
                break;
            case "exception":
                result = toIdentifier(node);
                break;
            case "case":
                result = new CaseStatementNode(node);
                break;
            default:
                break;
            }
        } else if (node.is(ScanResultType.CHARACTER)) {
            result = PlpgsqlLabel.from(node);
        }
        if (result == null) {
            result = interpretStatementStart(node);

            if (result instanceof IdentifierNode) {
                result = new JustAStatementNode(result);
            }
        }
        return result;
    }

    /**
     * Tries to find out what is created by a create statement and returns the appropriate return statement if there is
     * one. If no specific create statement can be found, then a JustAStatementNode will be returned.
     *
     * @param startNode
     *            The ScanResult that contains identifier "create"
     * @return SrcNode the specific create statement node or a JustAStatementNode.
     */
    private static SrcNode interpretCreateStatement(ScanResult startNode) {
        ScanResult nextWord;
        SrcNode result = null;

        boolean firstWordIsCreate = "create".equalsIgnoreCase(startNode.getText());
        if (firstWordIsCreate) {
            /*
             * Skip the "or replace" part if present
             */
            nextWord = startNode.getNextInterpretable();
            if (nextWord == null) {
                return new JustAStatementNode(startNode);
            }
            if (!ScanResultType.IDENTIFIER.equals(nextWord.getType())) {
                return new JustAStatementNode(startNode);
            }
            if ("or".equalsIgnoreCase(nextWord.getText())) {
                nextWord = nextWord.getNextInterpretable();
                if (nextWord == null) {
                    return new JustAStatementNode(startNode);
                }
                if (!ScanResultType.IDENTIFIER.equals(nextWord.getType())) {
                    return new JustAStatementNode(startNode);
                }
                if ("replace".equalsIgnoreCase(nextWord.getText())) {
                    nextWord = nextWord.getNextInterpretable();
                    if (nextWord == null) {
                        return new JustAStatementNode(startNode);
                    }
                    if (!ScanResultType.IDENTIFIER.equals(nextWord.getType())) {
                        return new JustAStatementNode(startNode);
                    }
                }
            }
        } else {
            nextWord = startNode;
        }

        /*
         * Here the nextWord is the identifier that defines the type of create statement
         */
        switch (nextWord.getText().toLowerCase()) {
        case "function":
        case "procedure":
            result = new CreateFunctionNode(startNode);
            break;
        case "type":
            return new CreateTypeNode(startNode);
        case "table":
        case "temporary":
        case "unlogged":
        case "temp":
        case "global":
            result = new CreateTableNode(startNode);
            break;
        default:
            result = new JustAStatementNode(startNode);
            break;
        }
        return result;
    }

    /**
     * Interprets a SELECT statement.
     * @param startNode The word SELECT or VALUES that starts this statement
     * @return The interpreted node
     */
    private static SrcNode interpretSelectStatement(ScanResult startNode) {
        return new SelectStatement(startNode);
    }

    /**
     * Interprets an identifier.
     * @param scanResult The identifier node that starts the name
     * @return The interpreted node
     */
    public static SrcNode interpretIdentifier(ScanResult scanResult) {
        IdentifierNode identifier = toIdentifier(scanResult);

        ScanResult node = identifier.getNextInterpretable();
        if (node == null) {
            return identifier;
        }

        switch (node.getType()) {
        case OPENING_PARENTHESIS:
            if ("(".equals(node.getText())) {
                if (ConfigUtil.isKeywordNotFunctionCall(identifier.getText().toUpperCase())) {
                    return identifier;
                }
                return new FunctionCallNode(identifier);
            }
            break;
        default:
            break;
        }

        return identifier;
    }

    /**
     * An identifier can start all kind of things. If it is followed by a dot, it may be a qualified name or a sequence
     * invocation.
     *
     * @param scanResult
     *            The identifier node that starts the name
     * @return One of IdentifierNode, DoubleQuotedIdentifierNode or QualifiedIdentifierNode
     */
    public static IdentifierNode toIdentifier(ScanResult scanResult) {
        IdentifierNode identifier = null;
        if (ScanResultType.IDENTIFIER.equals(scanResult.getType())) {
            identifier = new IdentifierNode(scanResult);
        } else {
            identifier = new DoubleQuotedIdentifierNode(scanResult);
        }

        ScanResult next = identifier.getNextInterpretable();
        if (next != null && next.is(ScanResultType.CHARACTER) && ".".equals(next.toString())) {
            return new QualifiedIdentifierNode(identifier);
        }
        return identifier;
    }

    /**
     * Checks if a character node can be turned into an operator.
     * <p>
     * A series of characters may represent an operator if they comply to the Postgres operator rules - see
     * {@linkplain https://www.postgresql.org/docs/current/sql-createoperator.html}. So if the startNode represents a
     * character that Postgres considers an operator character, then a check is made if following nodes are operator
     * characters as well. If so, then they are combined into a single {@link OperatorNode}.
     *
     * @param startNode
     *            The {@link ScanResult} of {@link ScanResultType#CHARACTER} that possibly represents (the start of) an
     *            operator
     * @return SrcNode An OperatorNode if the startNode (and its succeeding nodes?) can be turned into an operator.
     *         Otherwise a {@link CharacterNode}.
     */
    private static SrcNode interpretCharacter(ScanResult startNode) {
        String largestOperator = "";
        String text = "";
        ScanResult nextNode;
        assert ScanResultType.CHARACTER.equals(startNode
                .getType()) : "interpretCharacter(ScanResult startNode) MUST start with a ScanResult of type CHARACER";
        for (nextNode = startNode; nextNode != null
                && nextNode.is(ScanResultType.CHARACTER); nextNode = nextNode.getNext()) {
            text = text + nextNode.toString();
            if (OperatorNode.OPERATOR_CHARACTER_PATTERN.matcher(text).matches()) {
                largestOperator = text;
            } else {
                break;
            }
        }
        switch (largestOperator.length()) {
        case 0:
            if (":".contentEquals(startNode.getText()) && startNode.getNext() != null
                    && ":".contentEquals(startNode.getNext().getText())) {
                return new TypeCastNode(startNode);
            }
            return new CharacterNode(startNode);
        case 1:
            return new OperatorNode(startNode);
        default:
            break;
        }
        while (largestOperator.length() > 1 && (largestOperator.endsWith("+") || largestOperator.endsWith("-"))
                && !largestOperator.matches(".*[~!@#%\\^\\&|`?].*")) {
            largestOperator = largestOperator.substring(0, largestOperator.length() - 1);
        }
        if (largestOperator.length() == 1) {
            return new OperatorNode(startNode);
        }
        return new OperatorNode(largestOperator, startNode);
    }

    /**
     * Returns the first ScanResult from the input.
     * <p>
     * BEWARE! The first result can be obtained only ONCE. Following ScanResults can be obtained via the ScanResult's
     * getNext() method. This is to accommodate read ahead techniques to properly interpret the code. For example, to
     * interpret a statement that creates a function, the nodes CREATE - space - OR - space - REPLACE - space - FUNCTION
     * may need to be read while the statement will start at CREATE.
     *
     * @return ScanResult if invoked directly as first statement after the constructor or null in all other cases.
     */
    public ScanResult getFirstResult() {
        ScanResult result = scanResult;
        scanResult = null;
        return result;
    }

    @Override
    public void close() throws IOException {
        scanner.yyclose();
    }
}
