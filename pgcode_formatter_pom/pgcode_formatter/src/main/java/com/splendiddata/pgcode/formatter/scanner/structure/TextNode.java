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

import java.io.Serializable;

import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Just text to be added
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class TextNode extends SrcNode implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String text;

    /**
     * Constructor
     *
     * @param text
     *            The text to be added
     */
    public TextNode(String text) {
        super(ScanResultType.TEXT, null);
        this.text = text;
    }

    /**
     * @return String the text
     */
    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }
}
