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

import java.util.Deque;
import java.util.LinkedList;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CommaSeparatedListGroupingType;
import com.splendiddata.pgcode.formatter.scanner.structure.ArgumentDefinitionOffsets;

/**
 * Formatting context.
 */
public class FormatContext {
    private final FormatConfiguration config;
    private final FormatContext parentContext;
    private final Deque<Integer> indentStack = new LinkedList<>();

    private int availableWidth = Integer.MAX_VALUE;
    private int offset;
    private String language;
    private CommaSeparatedListGroupingType commaSeparatedListGroupingType;
    private ArgumentDefinitionOffsets argumentDefinitionOffsets;

    /**
     * Constructor
     *
     * @param config
     *            The FormatConfiguration to use for this context
     * @param context
     *            The parent parent FormatContext
     */
    public FormatContext(FormatConfiguration config, FormatContext context) {
        this.config = config;
        parentContext = context;

        if (context != null) {
            language = context.getLanguage();
            offset = context.getOffset();
            if (context.getAvailableWidth() < availableWidth) {
                availableWidth = context.getAvailableWidth();
            }
        }
        if (config != null) {
            if (config.getLineWidth().getValue() < availableWidth) {
                availableWidth = config.getLineWidth().getValue();
            }
        }
    }

    public FormatConfiguration getConfig() {
        return config;
    }

    /**
     * Returns the available width
     * @return The available width
     */
    public int getAvailableWidth() {
        return availableWidth;
    }

    /**
     * Set the available width
     * @param width The available width to set
     * @return this
     */
    public FormatContext setAvailableWidth(int width) {
        this.availableWidth = width;
        return this;
    }

    /**
     * Returns the offset
     * @return The offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Returns the language
     * @return The language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the language
     * @param language The language to set
     * @return this
     */
    public FormatContext setLanguage(String language) {
        this.language = language;
        return this;
    }

    /**
     * Return a string of spaces according to the current indentation level and the spaces setting for indenting.
     *
     * @return String spaces
     * @param newLine true for the standard indent, false to ge just a space
     */
    public static String indent(boolean newLine) {
        if (newLine) {
            return Util
                    .nSpaces(FormatConfiguration.getEffectiveConfiguration().getIndent().getIndentWidth().intValue());
        } else {
            return Util.space;
        }
    }

    /**
     * Sets the comma separate list grouping value to be used
     *
     * @param csArgumentGrouping
     *            The CommaSeparatedListGroupingType
     * @return FormatContext this
     */
    public FormatContext setCommaSeparatedListGrouping(CommaSeparatedListGroupingType csArgumentGrouping) {
        commaSeparatedListGroupingType = csArgumentGrouping;
        return this;
    }

    /**
     * returns the comma separate list grouping value to be used
     *
     * @return CommaSeparatedListGroupingType that was set using
     *         {@link #setCommaSeparatedListGrouping(CommaSeparatedListGroupingType)} or the comma separated list
     *         grouping from the provided config
     */
    public CommaSeparatedListGroupingType getCommaSeparatedListGrouping() {
        if (commaSeparatedListGroupingType == null) {
            return config.getCommaSeparatedListGrouping();
        }
        return commaSeparatedListGroupingType;
    }

    /**
     * @return ArgumentDefinitionOffsets the argumentDefinitionOffsets
     */
    public ArgumentDefinitionOffsets getArgumentDefinitionOffsets() {
        if (argumentDefinitionOffsets == null && parentContext != null) {
            return parentContext.getArgumentDefinitionOffsets();
        }
        return argumentDefinitionOffsets;
    }

    /**
     * @param argumentDefinitionOffsets
     *            the argumentDefinitionOffsets to set
     * @return FormatContext this
     */
    public FormatContext setArgumentDefinitionOffsets(ArgumentDefinitionOffsets argumentDefinitionOffsets) {
        this.argumentDefinitionOffsets = argumentDefinitionOffsets;
        return this;
    }

    /**
     * Returns the number of spaces that are currently to be used as indent
     *
     * @return int the number of spaces from the highest entry in the indent stack
     */
    public int getIndent() {
        if (indentStack.isEmpty()) {
            if (parentContext != null) {
                return parentContext.getIndent();
            }
            return 0;
        }
        return indentStack.peek().intValue();
    }

}
