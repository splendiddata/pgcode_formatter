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

package com.splendiddata.pgcode.formatter;

/**
 * A class for splitting a string to different parts
 */
public class SplitData {

    StringBuilder text;
    SplitDataType type = SplitDataType.TEXT;
    private String quoteString = "";

    /**
     * Default constructor
     */
    public SplitData() {
        text = new StringBuilder();
    }

    /**
     * Constructor
     *
     * @param type
     *            A {@link SplitDataType}
     * @param text
     *            A text as a String
     * @param quoteString
     *            Quoted String
     */
    public SplitData(SplitDataType type, String text, String quoteString) {
        this.type = type;
        this.text = new StringBuilder(text);
        this.quoteString = quoteString;
    }

    /**
     * @return Quoted String
     */
    public String getQuoteString() {
        return quoteString;
    }

    @Override
    public String toString() {
        String result = getText();
        if (SplitDataType.DOUBLE_QUOTED_IDENTIFIER.equals(type)) {
            result = getText();
        } else if (SplitDataType.LITERAL.equals(type)) {
            result = quoteString + getText() + quoteString;
        } else if (SplitDataType.ESCAPE_STRING.equals(type)) {
            result = "E" + '\'' + text + '\'';
        }

        return result;
    }

    public String getText() {
        return text.toString();
    }

    /**
     * Sets the text
     * 
     * @param text
     *            The text to set.
     * @return {@link SplitData} this
     */
    public SplitData setText(String text) {
        this.text = new StringBuilder(text);
        return this;
    }

    /**
     * Returns the type.
     * 
     * @return the type
     */
    public SplitDataType getType() {
        return type;
    }

    /**
     * Sets the type
     * 
     * @param type
     *            The type to set.
     * @return {@link SplitData} this
     */
    public SplitData setType(SplitDataType type) {
        this.type = type;
        return this;
    }

    /**
     * Appends the provided string to the text.
     * 
     * @param textPart
     *            The string to append to the text
     * @return {@link SplitData} this
     */
    public SplitData appendText(String textPart) {
        this.text.append(textPart);
        return this;
    }
}