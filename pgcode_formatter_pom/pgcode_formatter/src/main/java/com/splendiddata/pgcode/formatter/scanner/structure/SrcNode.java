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

    private RenderMultiLines cachedRenderResult;
    private FormatContext cachedContext;
    private int cachedParentPosition;

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
        RenderMultiLines cachedResult = getCachedRenderResult(formatContext, parentResult, config);
        if (cachedResult != null) {
            return cachedResult;
        }
        return cacheRenderResult(
                Util.renderStraightForward(getStartScanResult(),
                        new RenderMultiLines(this, formatContext, parentResult), formatContext, config),
                formatContext, parentResult);
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

    /**
     * Caches a clone of the resultToCache together with a clone of the formatContext and the current position in the
     * parentResult.
     * <p>
     * (Parts of) statements may be rendered multiple times in order to get a "best fit" in the end result. But when the
     * formatContext and the parent position do not change from one render attempt to another, then the render result
     * will not change either. So, in that case, a cached result could be returned.
     *
     * @param resultToCache
     *            The RenderResult of which a clone will be cached
     * @param formatContext
     *            The formatContext that probably influenced the rendering process. A clone if this will be cached as
     *            well.
     * @param parentResult
     *            The result to which the just rendered result would be added. The current position in that result may
     *            have influenced the rendering process. If the parentResult is null, then position zero will be
     *            assumed.
     * @return RenderResult The resultToCache
     * @since 0.3
     */
    protected RenderMultiLines cacheRenderResult(RenderMultiLines resultToCache, FormatContext formatContext,
            RenderMultiLines parentResult) {
        cachedRenderResult = resultToCache.clone();
        cachedContext = formatContext.clone();
        cachedParentPosition = 0;
        if (parentResult != null) {
            cachedParentPosition = parentResult.getPosition();
        }
        return resultToCache;
    }

    /**
     * Returns a clone of the cached result if
     * <ul>
     * <li>a render result has been cached using the
     * {@link #cacheRenderResult(RenderMultiLines, FormatContext, RenderMultiLines)} method</li>
     * <li>the formatContext equals the cached clone of the format context that was stored using the
     * cacheRenderResult(RenderMultiLines, FormatContext, RenderMultiLines) method.</li>
     * <li>the current position in the parentResult is the same as it was when the cacheRenderResult(RenderMultiLines,
     * FormatContext, RenderMultiLines) method was invoked or if the cachedRenderResult contains only one line and the
     * result would still fit on the line after the parent position</li>
     * </ul>
     *
     * @param formatContext
     *            The formatContext from the containing
     *            {@link #beautify(FormatContext, RenderMultiLines, FormatConfiguration)} method
     * @param parentResult
     *            The parentResult from the containing beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     *            method
     * @param config
     *            The configuration that will tell the maximum line width
     * @return RenderMultiLines a clone of the cached result or null if there wasn't any or if the cached result is no
     *         longer usefull
     * @since 0.3
     */
    protected RenderMultiLines getCachedRenderResult(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        if (cachedRenderResult == null) {
            return null;
        }
        int parentPosition = 0;
        if (parentResult != null) {
            parentPosition = parentResult.getPosition();
        }
        if (cachedContext.equals(formatContext)
                && (cachedParentPosition == parentPosition || cachedRenderResult.getHeight() <= 1
                        && (parentPosition < cachedParentPosition || (parentPosition > cachedParentPosition
                                && cachedRenderResult.getWidth() <= config.getLineWidth().getValue())))) {
            return cachedRenderResult.clone();
        }
        return null;
    }

    /**
     * Clears the cached result
     *
     * @param <T>
     *            The actual class
     * @return this
     * @since 0.3
     */
    @SuppressWarnings("unchecked")
    public <T extends SrcNode> T clearResultCache() {
        cachedRenderResult = null;
        cachedContext = null;
        return (T) this;
    }
}
