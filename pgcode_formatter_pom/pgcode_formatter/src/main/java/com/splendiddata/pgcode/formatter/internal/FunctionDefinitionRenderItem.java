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

/**
 * A function definition render item class that is meant to render a the function definition as single string.
 */
public class FunctionDefinitionRenderItem extends RenderItem {

    /**
     * The height (i.e. the number of lines) of the text in this render item.
     */
    int height;

    /**
     * The width of the text in this render item.
     */
    int width;

    /**
     * Constructor without ScanResult (i.e. uses null as ScanResult).
     *
     * @param nonBreakableText
     *            A string that contains the text to render.
     * @param renderItemType
     *            The {@link RenderItemType} of the result to render.
     */
    public FunctionDefinitionRenderItem(String nonBreakableText, RenderItemType renderItemType) {
        super(nonBreakableText, renderItemType);
    }

    @Override
    public int getWidth() {
        return width;
    }

    /**
     * Sets the width of this FunctionDefinitionRenderItem.
     * 
     * @param width
     *            the width to set
     * @return the width of this FunctionDefinitionRenderItem.
     */
    public FunctionDefinitionRenderItem setWidth(int width) {
        this.width = width;
        return this;
    }

    @Override
    public int getHeight() {
        return height;
    }

    /**
     * Sets the height (i.e. number of lines) of this FunctionDefinitionRenderItem.
     * 
     * @param height
     *            the height to set
     * @return the height (i.e. number of lines) of this FunctionDefinitionRenderItem.
     */
    public FunctionDefinitionRenderItem setHeight(int height) {
        this.height = height;
        return this;
    }
}
