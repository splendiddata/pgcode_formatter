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

package com.splendiddata.pgcode.formatter.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Wrapper class for (error) messages.
 *
 * @author Splendid Data Product Development B.V.
 */
public class Msg implements Serializable {
    private static final long serialVersionUID = 2L;

    private final Serializable format;

    private final Serializable[] arguments;

    /**
     * Constructor
     *
     * @param format
     *            The MsgKey that identifies the {@link java.text.MessageFormat} string in a resource bundle
     * @param arguments
     *            The positional arguments to be inserted into the format.
     */
    public Msg(MsgKey format, Serializable... arguments) {
        this.format = format;
        this.arguments = arguments;
    }

    /**
     * Constructor
     *
     * @param format
     *            The MessageFormat as String.
     * @param arguments
     *            The positional arguments to be inserted into the format.
     */
    public Msg(String format, Serializable... arguments) {
        this.format = format;
        this.arguments = arguments;
    }

    /**
     * Copy constructor
     *
     * @param original
     *            The Msg of which to make a shallow copy
     */
    public Msg(com.splendiddata.pgcode.formatter.util.Msg original) {
        this.format = original.getFormat();
        this.arguments = original.getArguments();
    }

    /**
     * @return Serializable Most likely this will be a MsgKey. But it might be a String as well. If there are arguments
     *         specified in this Msg, the format (probably obtained from a ResourceBundle when the format appeared to be
     *         a MsgKey) will be passed to {@link java.text.MessageFormat#format(String, Object...)} together with the
     *         arguments.
     */
    public Serializable getFormat() {
        return format;
    }

    /**
     * @return Serializable[] The arguments that are to be passed to
     *         {@link java.text.MessageFormat#format(String, Object...)} as object...
     */
    public Serializable[] getArguments() {
        return Arrays.copyOf(arguments, arguments.length);
    }

    /**
     * Dumps the content into a String for debugging purposes
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(getClass().getSimpleName()).append(":");
        result.append(" format=").append(format);
        if (arguments.length > 0) {
            String sep = " arguments={";
            for (Serializable arg : arguments) {
                result.append(sep).append(arg);
                sep = ", ";
            }
            result.append('}');
        }
        return result.toString();
    }

    @Override
    public int hashCode() {
        return format.hashCode() ^ Arrays.deepHashCode(arguments);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof com.splendiddata.pgcode.formatter.util.Msg) {
            return (format == ((com.splendiddata.pgcode.formatter.util.Msg) other).format
                    || Objects.equals(format, ((com.splendiddata.pgcode.formatter.util.Msg) other).format))
                    && Arrays.deepEquals(arguments,
                            ((com.splendiddata.pgcode.formatter.util.Msg) other).arguments);
        }
        return false;
    }
}