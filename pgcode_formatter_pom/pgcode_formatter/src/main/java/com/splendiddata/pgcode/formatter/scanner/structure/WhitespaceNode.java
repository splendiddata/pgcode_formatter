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
import com.splendiddata.pgcode.formatter.internal.*;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A white space.
 */
public class WhitespaceNode extends SrcNode {

    private final String whitespace;

    /**
     * Constructor
     *
     * @param scanResult
     *            The scan result that is containing just whitespace
     */
    public WhitespaceNode(ScanResult scanResult) {
        super(ScanResultType.WHITESPACE, scanResult);
        this.whitespace = Util.space;
    }

    /**
     * Constructor
     *
     * @param string
     *            A whitespace to create a gap
     */
    public WhitespaceNode(String string) {
        super(ScanResultType.WHITESPACE, null);
        this.whitespace = string;
    }

    /**
     * Constructor
     *
     * @param string
     *            Whitespace that is to be printed instead of whatever is in the scanResult
     * @param scanResult
     *            The scanResult that is to be overwritten with whitespace
     */
    public WhitespaceNode(String string, ScanResult scanResult) {
        super(ScanResultType.WHITESPACE, scanResult);
        this.whitespace = string;
    }

    /**
     * @return String the whitespace
     */
    public final String getWhitespace() {
        return whitespace;
    }

    @Override
    public String toString() {
        return whitespace;
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        return new RenderItem(toString(), getStartScanResult(), RenderItemType.WHITESPACE);
    }
}
