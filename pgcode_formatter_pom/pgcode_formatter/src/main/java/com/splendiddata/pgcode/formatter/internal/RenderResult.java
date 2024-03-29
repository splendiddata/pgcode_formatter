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

package com.splendiddata.pgcode.formatter.internal;

/**
 * Interface for the formatted code that has to be rendered.
 */
public interface RenderResult extends Cloneable {

    /**
     * Returns the height of this render result. In case of A {@link RenderItem}, the height is 1 because it has a
     * single line.
     * 
     * @return The height of a {@link RenderResult} element.
     */
    int getHeight();

    /**
     * Returns the width of this render result. In case of A {@link RenderItem}, the width is the width of a single
     * line.
     *
     * @return The height of a {@link RenderResult} element.
     */
    int getWidth();

    /**
     * Returns the width of the first line of a {@link RenderResult}.
     * 
     * @return The width of the first line of a {@link RenderResult}.
     */
    int getWidthFirstLine();

    /**
     * Returns the {@link RenderItemType} of a RenderResult.
     * 
     * @return The {@link RenderItemType} of a RenderResult.
     */
    RenderItemType getRenderItemType();

    /**
     * Returns the beautified {@link RenderResult} as a string
     * 
     * @return The beautified result a string.
     */
    String beautify();

    /**
     * Checks whether the last non white space item in a {@link RenderResult} is a RenderItem of type LINEFEED.
     * 
     * @return True when the last non white space item in a {@link RenderResult} is a RenderItem of type LINEFEED.
     */
    boolean isLastNonWhiteSpaceEqualToLinefeed();

    /**
     * @see java.lang.Object#clone()
     *
     * @return RenderResult
     */
    RenderResult clone();

}
