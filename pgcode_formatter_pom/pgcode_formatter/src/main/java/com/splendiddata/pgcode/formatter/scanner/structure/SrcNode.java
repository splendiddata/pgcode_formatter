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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.internal.Util;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Base type for source nodes
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public abstract class SrcNode implements ScanResult {
    private static final Logger log = LogManager.getLogger(SrcNode.class);
    private final ScanResultType type;
    private ScanResult startScanResult;

    private ScanResult next;

    /**
     * Constructor
     *
     * @param type
     *            Type of node
     * @param scanResult
     *            The source of this node (may be null)
     */
    protected SrcNode(ScanResultType type, ScanResult scanResult) {
        this.type = type;
        startScanResult = scanResult;
        if (scanResult == null) {
            this.next = null;
        } else {
            this.next = scanResult.getNext();
        }
    }

    /**
     * Copy constructor
     * <p>
     * Copies the toCopy SrcNode, but sets nextScanResult in the new SrcNode to null. This is because copied SrcNodes
     * will never appear in the scanning process itself, so the reference to the next will be obsolete.
     * </p>
     *
     * @param toCopy
     *            The SrcNode to copy
     */
    protected SrcNode(SrcNode toCopy) {
        type = toCopy.type;
        startScanResult = toCopy.startScanResult;
        this.next = null;
    }

    /**
     * @return ScanResult the startScanResult
     */
    public final ScanResult getStartScanResult() {
        return startScanResult;
    }

    /**
     * @return ScanResult the next
     */
    @Override
    public final ScanResult getNext() {
        return this.next;
    }

    /**
     * @param next
     *            the next to set
     */
    @Override
    public final void setNext(ScanResult next) {
        this.next = next;
    }

    @Override
    public String getText() {
        return startScanResult.getText();
    }

    @Override
    public ScanResultType getType() {
        return type;
    }

    @Override
    public int getParenthesisLevel() {
        return startScanResult.getParenthesisLevel();
    }

    @Override
    public int getBeginEndLevel() {
        return startScanResult.getBeginEndLevel();
    }

    @Override
    public boolean isStatementEnd() {
        return startScanResult.isStatementEnd();
    }

    @Override
    public boolean isEof() {
        return startScanResult.isEof();
    }

    /**
     * @see java.lang.Object#toString()
     *
     * @return String The content of this srcNode
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (ScanResult node = getStartScanResult(); node != null && node != getNext(); node = node.getNext()) {
            result.append(node);
        }
        return result.toString();
    }

    /**
     * This implementation starts with the startScanResult and renders every node that it can find via the getNext()
     * queue
     * 
     * @see ScanResult#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     *
     * @param formatContext
     *            may contain hints on how to format
     * @param parentResult
     *            The RenderMultiLines that will receive the result of this method
     * @param config
     *            The configuration that tells how to format in a standard way
     * @return RenderResult
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        if (log.isTraceEnabled()) {
            log.trace("beautify called from" + Thread.currentThread().getStackTrace()[2]);
        }
        return Util.renderStraightForward(getStartScanResult(), new RenderMultiLines(this, formatContext),
                formatContext, config);
    }

    /**
     * Replaces the pointer to the startScanResult with the replacement
     *
     * @param replacement
     *            The probably re-interpreted ScanResult that is to be registered as startScanResult
     */
    protected void replaceStartScanResult(ScanResult replacement) {
        startScanResult = replacement;
    }
}
