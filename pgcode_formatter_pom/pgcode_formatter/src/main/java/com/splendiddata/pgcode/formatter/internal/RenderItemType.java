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
 * Types of render results
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public enum RenderItemType {
    /**
     * A character
     */
    CHARACTER,

    /**
     * A C-style block comment.
     */
    COMMENT,

    /**
     * A line comment, i.e. beginning with double dashes
     */
    COMMENT_LINE,

    OPERATOR,

    /**
     * Text contains just whitespace
     */
    WHITESPACE,

    /**
     * The text contains an unquoted (key?)word
     */
    IDENTIFIER,

    COMBINED_IDENTIFIER,

    /**
     * Probably an identifier, found between double quotes
     * <p>
     * Note that the double quotes are stripped off
     * </p>
     */
    DOUBLE_QUOTED_IDENTIFIER,

    /**
     * Just text found between single quotes
     * <p>
     * Note that the quotes are stripped off
     * </p>
     */
    LITERAL,

    ESCAPE_STRING,

    /**
     * End o statement
     */
    SEMI_COLON,

    /**
     * New line
     */
    LINEFEED,

    /**
     * The signature of a function or procedure. This includes the function name, parameters and return type
     */
    FUNCTION_SIGNATURE,

    /**
     * Definition of a function or a procedure
     */
    FUNCTION_DEFINITION;
}
