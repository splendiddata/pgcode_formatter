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

import java.util.LinkedList;

import com.splendiddata.pgcode.formatter.scanner.ScanResult;

/**
 * A render item class. It is meant to render a single string.
 */
public class RenderItem implements RenderResult {
    private String nonBreakableText;
    private ScanResult startScanResult;
    private RenderItemType renderItemType;


    /**
     * Constructor.
     * 
     * @param nonBreakableText
     *            A string that contains the text to render.
     * @param scanResult
     *            If not null, the start node that contains the nonBreakableText.
     * @param renderItemType
     *            The {@link RenderItemType} of the result to render.
     */
    public RenderItem(String nonBreakableText, ScanResult scanResult, RenderItemType renderItemType) {
        this.nonBreakableText = nonBreakableText;
        this.startScanResult = scanResult;
        this.renderItemType = renderItemType;
    }

    /**
     * Constructor without ScanResult (i.e. uses null as ScanResult).
     * 
     * @param nonBreakableText
     *            A string that contains the text to render.
     * @param renderItemType
     *            The {@link RenderItemType} of the result to render.
     */
    public RenderItem(String nonBreakableText, RenderItemType renderItemType) {
        this(nonBreakableText, null, renderItemType);
    }
    

    /**
     * @see java.lang.Object#clone()
     *
     * @return RenderItem the clone
     */
    @Override
    public RenderItem clone() {
        try {
            return (RenderItem)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("RenderItem appeared not cloneable after all", e);
        }
    }

    @Override
    public ScanResult getStartScanResult() {
        return startScanResult;
    }

    /**
     * Returns the text of the RenderItem.
     * 
     * @return The text of the RenderItem.
     */
    public String getNonBreakableText() {
        return nonBreakableText;
    }

    /**
     * Set the text in of the RenderItem.
     * 
     * @param nonBreakableText
     *            The text to set.
     * @return RenderItem this.
     */
    public RenderItem setNonBreakableText(String nonBreakableText) {
        this.nonBreakableText = nonBreakableText;
        return this;
    }

    /**
     * Returns the height (i.e. number of lines) of this RenderItem. It contains one single line, so the height is equal
     * to 1.
     * 
     * @return the height of this RenderItem.
     */
    @Override
    public int getHeight() {
        return 1; // Always a single line
    }

    /**
     * Returns the width of the text in this RenderItem.
     * 
     * @return the width.
     */
    @Override
    public int getWidth() {
        return renderItemType == RenderItemType.LINEFEED ? 0 : nonBreakableText.length();
    }

    /**
     * Returns the width of the text in this RenderItem; it contains one single line.
     * 
     * @return the width.
     */
    @Override
    public int getWidthFirstLine() {
        return getWidth();
    }

    @Override
    public RenderItemType getRenderItemType() {
        return renderItemType;
    }

    @Override
    public String toString() {
        return nonBreakableText;
    }

    @Override
    public String beautify() {
        return nonBreakableText;
    }

    /**
     * Returns the {@link RenderResult} this.
     *
     * @return The {@link RenderResult} this.
     */
    @Override
    public RenderResult getLast() {
        return this;
    }

    /**
     * There is only one RenderItem, so return this.
     * 
     * @return RenderItem this.
     */
    @Override
    public RenderResult getFirst() {
        return this;
    }

    /**
     * In case of RenderItem, it checks whether the render item type is RenderItemType.LINEFEED
     * 
     * @return true if the render item type is RenderItemType.LINEFEED
     */
    public boolean isLastNonWhiteSpaceEqualToLinefeed() {
        return RenderItemType.LINEFEED.equals(renderItemType);
    }

    /**
     * @throws UnsupportedOperationException
     *             whenever invoked
     * @see com.splendiddata.pgcode.formatter.internal.RenderResult#addLineAtStart(boolean)
     */
    @Override
    public RenderResult addLineAtStart(boolean lineExists) {
        throw new UnsupportedOperationException(
                "com.splendiddata.pgcode.formatter.scanner.structure.RenderItem.addLineAtStart(boolean) is not implemented");
    }

    /**
     * Returns a list containing one {@link RenderItem} element.
     *
     * @return A list that contains {@link RenderItem} this.
     */
    public LinkedList<RenderItem> getRenderItems() {
        LinkedList<RenderItem> renderItems = new LinkedList<>();
        renderItems.add(this);
        return renderItems;
    }

}
