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
 * Open a cursor clause
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class OpenCursorNode extends SrcNode {
    private final List<SrcNode> constituentParts = new LinkedList<>();
    private final SrcNode name;

    /**
     * Constructor
     *
     * @param openCursor
     *            The "open" identifier
     */
    public OpenCursorNode(ScanResult openCursor) {
        super(ScanResultType.CURSOR_DECLARATION, openCursor);

        ScanResult current = openCursor;
        IdentifierNode identifierNode = PostgresInputReader.toIdentifier(current);
        constituentParts.add(identifierNode);
        current = identifierNode.getNext();

        while (!current.getType().isInterpretable() && !current.isEof()) {
            SrcNode srcNode = PostgresInputReader.interpretStatementBody(current);
            constituentParts.add(srcNode);
            current = srcNode.getNext();
        }
        name = PostgresInputReader.interpretStatementStart(current);
        current = name.getNext();
        name.setNext(null);

        constituentParts.add(name);

        if (ScanResultType.IDENTIFIER.equals(current.getType())) {
            if ("no".equalsIgnoreCase(current.getText())) {
                // NO
                identifierNode = new IdentifierNode(current);
                constituentParts.add(new WhitespaceNode(" "));
                constituentParts.add(identifierNode);
                current = current.getNextInterpretable();
            }
        }

        if (ScanResultType.IDENTIFIER.equals(current.getType())) {
            if ("scroll".equalsIgnoreCase(current.getText())) {
                // SCROLL
                identifierNode = new IdentifierNode(current);
                constituentParts.add(new WhitespaceNode(" "));
                constituentParts.add(identifierNode);
                current = current.getNextInterpretable();
            }
        }

        while (!current.isStatementEnd()) {
            SrcNode interpreted = PostgresInputReader.interpretStatementBody(current);
            constituentParts.add(interpreted);
            current = interpreted.getNext();
            interpreted.setNext(null);
        }
        setNext(current);

    }

    /**
     * @return SrcNode the name of the cursor.
     */
    public final SrcNode getName() {
        return name;
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        RenderMultiLines result = new RenderMultiLines(this, formatContext, parentResult);

        for (SrcNode srcNode : constituentParts) {
            RenderResult beautified = srcNode.beautify(formatContext, result, config);
            boolean commaSeparatedListMultiLine = beautified.getWidth() > config
                    .getCommaSeparatedListGrouping().getMaxSingleLineLength().getValue();
            if (commaSeparatedListMultiLine) {
                result.addLine();
            }
            result.addRenderResult(beautified, formatContext);

        }

        return result;
    }
}
