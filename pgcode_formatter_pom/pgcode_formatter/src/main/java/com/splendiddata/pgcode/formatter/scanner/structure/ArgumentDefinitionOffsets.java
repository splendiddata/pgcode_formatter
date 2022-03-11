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

package com.splendiddata.pgcode.formatter.scanner.structure;

import jakarta.xml.bind.annotation.XmlAttribute;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;

import com.splendiddata.pgcode.formatter.internal.Util;

/**
 * Some offsets that can be used in the argument layout in a function definition
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public final class ArgumentDefinitionOffsets implements Cloneable {
    private Integer nameOffset;
    private Integer dataTypeOffset;
    private Integer defaultIndicatorOffset;
    private Integer defaultExpressionOffset;

    /**
     * Constructor
     */
    public ArgumentDefinitionOffsets() {
        // empty
    }

    /**
     * @see java.lang.Object#clone()
     *
     * @return ArgumentDefinitionOffsets
     */
    @Override
    public ArgumentDefinitionOffsets clone() {
        try {
            return (ArgumentDefinitionOffsets) super.clone();
        } catch (CloneNotSupportedException e) {
            LogManager.getLogger(getClass()).fatal(e, e);
            return null;
        }
    }

    /**
     * @see java.lang.Object#toString()
     *
     * @return String with the content for debugging purposes
     */
    @Override
    public String toString() {
        return Util.xmlBeanToString(this);
    }

    /**
     * @return Integer the nameOffset
     */
    @XmlAttribute
    public Integer getNameOffset() {
        return nameOffset;
    }

    /**
     * @param nameOffset
     *            the nameOffset to set
     */
    public void setNameOffset(Integer nameOffset) {
        this.nameOffset = nameOffset;
    }

    /**
     * @return Integer the dataTypeOffset
     */
    @XmlAttribute
    public Integer getDataTypeOffset() {
        return dataTypeOffset;
    }

    /**
     * @param dataTypeOffset
     *            the dataTypeOffset to set
     */
    public void setDataTypeOffset(Integer dataTypeOffset) {
        this.dataTypeOffset = dataTypeOffset;
    }

    /**
     * @return Integer the defaultIndicatorOffset
     */
    @XmlAttribute
    public Integer getDefaultIndicatorOffset() {
        return defaultIndicatorOffset;
    }

    /**
     * @param defaultIndicatorOffset
     *            the defaultIndicatorOffset to set
     */
    public void setDefaultIndicatorOffset(Integer defaultIndicatorOffset) {
        this.defaultIndicatorOffset = defaultIndicatorOffset;
    }

    /**
     * @return Integer the defaultExpressionOffset
     */
    @XmlAttribute
    public Integer getDefaultExpressionOffset() {
        return defaultExpressionOffset;
    }

    /**
     * @param defaultExpressionOffset
     *            the defaultExpressionOffset to set
     */
    public void setDefaultExpressionOffset(Integer defaultExpressionOffset) {
        this.defaultExpressionOffset = defaultExpressionOffset;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArgumentDefinitionOffsets) {
            ArgumentDefinitionOffsets other = (ArgumentDefinitionOffsets) obj;
            return Objects.equals(this.getDataTypeOffset(), other.getDataTypeOffset())
                    && Objects.equals(this.getDefaultExpressionOffset(), other.getDefaultExpressionOffset())
                    && Objects.equals(this.getDefaultIndicatorOffset(), other.getDefaultIndicatorOffset())
                    && Objects.equals(this.getNameOffset(), other.getNameOffset());
        }
        return false;
    }

}
