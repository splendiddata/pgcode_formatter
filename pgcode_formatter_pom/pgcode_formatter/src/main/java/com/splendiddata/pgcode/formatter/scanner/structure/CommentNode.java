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

import java.util.regex.Pattern;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A C-style block comment.
 */
public class CommentNode extends SrcNode {
    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\n");
    private final String comment;

    /**
     * Constructor
     *
     * @param scanResult
     *            The source node that will provide the comment
     */
    public CommentNode(ScanResult scanResult) {
        super(ScanResultType.COMMENT, scanResult);
        this.comment = scanResult.getText();
    }

    /**
     * @return String the comment
     */
    public final String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        return comment;
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {

        String[] lines = NEWLINE_PATTERN.split(comment);
        if (lines.length == 1) {
            return new RenderItem(comment, this, RenderItemType.COMMENT);
        }
        RenderMultiLines result = new RenderMultiLines(this, formatContext)
                .setIndent(parentResult.getPosition() - parentResult.getStandardIndent() + 1);
        boolean first = true;
        for (String line : lines) {
            if (first) {
                first = false;
            } else {
                result.addLine();
            }
            result.addRenderResult(new RenderItem(line, RenderItemType.COMMENT), formatContext);
        }
        return result;
    }
}