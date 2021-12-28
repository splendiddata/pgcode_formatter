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

import java.util.function.Function;
import java.util.regex.Pattern;

import com.splendiddata.pgcode.formatter.ConfigUtil;
import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Can be a when clause of a case clause or a when part in a case statement or a when statement in exception handling
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class WhenClauseNode extends SrcNode {
    private static final Pattern END_OF_WHEN_PATTERN = Pattern.compile("^(WHEN|ELSE|END)$", Pattern.CASE_INSENSITIVE);
    private SrcNode whenExpression;
    private ScanResult thenExpression;

    /**
     * Constructor
     *
     * @param startNode
     *            The node that starts this when statement or clause. This may be whitespace or comment, but the first
     *            effective node must be 'WHEN'.
     * @param interpreter
     *            The interpreter to use for the content of the when clause.
     */
    public WhenClauseNode(ScanResult startNode, Function<ScanResult, SrcNode> interpreter) {
        super(ScanResultType.WHEN_THEN_CLAUSE, startNode);
        ScanResult cur = startNode;
        if (!"when".equalsIgnoreCase(cur.toString())) {
            // a when statement may start with comment
            cur = cur.getNextInterpretable();
        }
        assert "when".equalsIgnoreCase(cur.toString()) : "Expecting 'WHEN' but got: " + cur;
        whenExpression = PostgresInputReader.interpretStatementBody(cur.getNext());
        cur.setNext(null);
        ScanResult prior = whenExpression;
        for (cur = whenExpression.getNext(); !cur.isEof()
                && !"then".equalsIgnoreCase(cur.toString()); cur = prior.getNext()) {
            cur = PostgresInputReader.interpretStatementBody(cur);
            prior.setNext(cur);
            prior = cur;
        }
        prior.setNext(null);
        if (cur.isEof()) {
            setNext(cur);
            return;
        }
        thenExpression = cur;
        for (cur = cur.getNextInterpretable(); !cur.isEof()
                && !END_OF_WHEN_PATTERN.matcher(cur.toString()).matches(); cur = cur.getNextInterpretable()) {
            if (cur.isStatementEnd()) {
                break;
            }
        }
        prior = thenExpression;
        for (cur = thenExpression.getNext(); cur != null && !cur.isEof()
                && !END_OF_WHEN_PATTERN.matcher(cur.toString()).matches(); cur = prior.getNext()) {
            if (cur.getType().isInterpretable()) {
                cur = interpreter.apply(cur);
            }
            prior.setNext(cur);
            prior = cur;
        }
        prior.setNext(null);
        setNext(cur);
    }

    /**
     * @see java.lang.Object#toString()
     *
     * @return String for debugging purposes
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            str.append(node);
        }
        for (ScanResult node = whenExpression; node != null; node = node.getNext()) {
            str.append(node);
        }
        for (ScanResult node = thenExpression; node != null; node = node.getNext()) {
            str.append(node);
        }
        return str.toString();
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        RenderMultiLines result = new RenderMultiLines(this, formatContext);

        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            RenderResult renderResult = node.beautify(formatContext, result, config);
            result.addRenderResult(renderResult, formatContext);
        }
        for (ScanResult node = whenExpression; node != null; node = node.getNext()) {
            RenderResult res = node.beautify(formatContext, result, config);
            result.addRenderResult(res, formatContext);
        }

        for (ScanResult node = thenExpression; node != null; node = node.getNext()) {
            RenderResult res = node.beautify(formatContext, result, config);

            if (node instanceof IdentifierNode && "then".equalsIgnoreCase(node.toString())) {
                boolean onSeparateLine = config.getQueryConfig().isMajorKeywordsOnSeparateLine().booleanValue()
                        && ConfigUtil.isMajorKeywords(node.getText());
                switch (formatContext.getCaseType().getThenPosition().getValue()) {
                case THEN_AFTER_WHEN_DIRECTLY: // on the same line immediately following the WHEN condition
                    // do nothing special, just add
                    result.addRenderResult(res, formatContext);
                    break;
                case THEN_AFTER_WHEN_ALIGNED: // first THEN on the same line but the following THEN's are aligned vertically
                    result.addRenderResult(res, formatContext);

                    break;
                case THEN_INDENTED:
                    result.setIndent(FormatContext.indent(true));
                    result.addLine();
                    result.addRenderResult(res, formatContext);
                    break;
                case THEN_UNDER_WHEN: // THEN on the next line directly under WHEN
                    result.addLine();
                    result.addRenderResult(res, formatContext);
                    break;
                default:
                    if (onSeparateLine) {
                        result.addLine();
                        result.addRenderResult(res, formatContext);
                        result.addLine();
                    } else {
                        result.addRenderResult(res, formatContext);
                    }
                    result.addRenderResult(res, formatContext);

                    break;
                }
            } else {
                result.addRenderResult(res, formatContext);
            }
        }

        result.removeTrailingSpaces();
        return result;
    }
}
