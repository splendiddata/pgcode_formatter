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

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A line comment; a standard SQL comment
 */
public class CommentLineNode extends SrcNode {
    private final String comment;

    /**
     * Constructor
     *
     * @param scanResult
     *            The source node that will provide the comment
     */
    public CommentLineNode(ScanResult scanResult) {
        super(ScanResultType.COMMENT_LINE, null);
        if (scanResult instanceof CommentLineNode) {
            this.comment = ((CommentLineNode) scanResult).comment;
            this.replaceStartScanResult(((CommentLineNode) scanResult).getStartScanResult());
            this.setNext(scanResult.getNext());
        } else {
            this.comment = scanResult.getText().trim();
            this.replaceStartScanResult(scanResult);
            ScanResult lf = scanResult.getNext();
            if (lf != null && lf.is(ScanResultType.LINEFEED)) {
                setNext(lf.getNext());
                // Normally, we should have set next of 'lf' to null, but because this constructor
                // is called from ScanResult.beautify() from where this.next can not be passed on,
                // we skip lf.setNext(null).
            } else {
                setNext(lf);
                // Normally, we should have set next of 'scanResult' to null, but because this constructor
                // is called from ScanResult.beautify() from where this.next can not be passed on,
                // we skip scanResult.setNext(null).
            }
        }
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        if (parentResult == null) {
            return new RenderMultiLines(this, formatContext, null).preserveLineFeed()
                    .addRenderResult(new RenderItem(comment, this, RenderItemType.COMMENT_LINE), formatContext)
                    .addLine();
        }
        parentResult.addEolComment(comment);
        return new RenderItem("", RenderItemType.WHITESPACE);
    }

    @Override
    public String toString() {
        return getStartScanResult().toString();
    }

    /**
     * Sets the indicator whether on separate line or not.
     * 
     * @param onSeparateLine
     *            indicates whether on separate line or not
     * @return {@link CommentLineNode} this
     */
    public CommentLineNode setOnSeparateLine(boolean onSeparateLine) {
        return this;
    }
}