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

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.ConfigUtil;
import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CommaSeparatedListGroupingType;
import com.splendiddata.pgcode.formatter.scanner.structure.ArgumentDefinitionOffsets;

/**
 * Formatting context.
 * <p>
 * Contains settings that may vary because of the context in which they are used.
 */
public class FormatContext implements Cloneable {
    private static final Logger log = LogManager.getLogger(FormatContext.class);

    private final FormatContext parentContext;

    private int availableWidth = Integer.MAX_VALUE;
    private String language;
    private CommaSeparatedListGroupingType commaSeparatedListGroupingType;
    private ArgumentDefinitionOffsets argumentDefinitionOffsets;

    /**
     * Constructor
     *
     * @param config
     *            The FormatConfiguration from which the commaSeparatedListGroupingType is taken if the context argument
     *            is null
     * @param context
     *            The parent parent FormatContext
     */
    public FormatContext(FormatConfiguration config, FormatContext context) {
        parentContext = context;

        if (context != null) {
            language = context.getLanguage();
            if (context.getAvailableWidth() < availableWidth) {
                availableWidth = context.getAvailableWidth();
            }
            commaSeparatedListGroupingType = ConfigUtil.copy(context.commaSeparatedListGroupingType);
            if (context.argumentDefinitionOffsets != null) {
                argumentDefinitionOffsets = context.argumentDefinitionOffsets.clone();
            }
        }
        if (config != null) {
            if (config.getLineWidth().getValue() < availableWidth) {
                availableWidth = config.getLineWidth().getValue();
            }
            commaSeparatedListGroupingType = config.getCommaSeparatedListGrouping();
        }
    }

    /**
     * Copy constructor
     *
     * @param original
     *            The FormatContext to copy
     */
    public FormatContext(FormatContext original) {
        assert original != null : "new FormatContext(null) not allowed";
        parentContext = original.parentContext;
        commaSeparatedListGroupingType = ConfigUtil.copy(original.getCommaSeparatedListGrouping());
        language = original.getLanguage();
        availableWidth = original.getAvailableWidth();
        if (original.argumentDefinitionOffsets != null) {
            argumentDefinitionOffsets = original.argumentDefinitionOffsets.clone();
        }
    }

    /**
     * Returns the available width
     * 
     * @return The available width
     */
    public int getAvailableWidth() {
        return availableWidth;
    }

    /**
     * Set the available width
     * 
     * @param width
     *            The available width to set
     * @return this
     */
    public FormatContext setAvailableWidth(int width) {
        this.availableWidth = width;
        return this;
    }

    /**
     * Returns the language
     * 
     * @return The language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the language
     * 
     * @param language
     *            The language to set
     * @return this
     */
    public FormatContext setLanguage(String language) {
        this.language = language;
        return this;
    }

    /**
     * Sets the comma separate list grouping value to be used
     *
     * @param csArgumentGrouping
     *            The CommaSeparatedListGroupingType
     * @return FormatContext this
     */
    public FormatContext setCommaSeparatedListGrouping(CommaSeparatedListGroupingType csArgumentGrouping) {
        assert csArgumentGrouping != null : "setCommaSeparatedListGrouping(null) not allowed";
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
     * Does a deep equals
     * 
     * @see Object#equals(java.lang.Object)
     */
    public boolean equals(Object otherObject) {
        if (otherObject == this) {
            return true;
        }
        if (!(otherObject instanceof FormatContext)) {
            return false;
        }
        FormatContext other = (FormatContext) otherObject;
        if (this.availableWidth != other.availableWidth) {
            return false;
        }
        if (!Objects.equals(this.language, other.language)) {
            return false;
        }
        if (!ConfigUtil.equals(this.commaSeparatedListGroupingType, other.commaSeparatedListGroupingType)) {
            return false;
        }
        if (!Objects.equals(this.argumentDefinitionOffsets, other.argumentDefinitionOffsets)) {
            return false;
        }
        if (!Objects.equals(this.parentContext, other.parentContext)) {
            return false;
        }

        return true;
    }

    /**
     * @see java.lang.Object#clone()
     */
    public FormatContext clone() {
        try {
            FormatContext clone = (FormatContext) super.clone();
            if (this.commaSeparatedListGroupingType != null) {
                clone.commaSeparatedListGroupingType = ConfigUtil.copy(this.commaSeparatedListGroupingType);
            }
            if (this.argumentDefinitionOffsets != null) {
                clone.argumentDefinitionOffsets = this.argumentDefinitionOffsets.clone();
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            log.error("clone didn't work", e);
        }
        return null;
    }
}
