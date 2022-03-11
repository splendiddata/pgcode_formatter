/*
 * Copyright (c) Splendid Data Product Development B.V. 2021 - 2022
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

import com.splendiddata.pgcode.formatter.ConfigUtil;
import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.CaseType;

/**
 * Format context for case clauses and case statements
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.3
 */
public class CaseFormatContext extends FormatContext {
    /**
     * Indication of the rendering phase
     */
    public static enum RenderPhase {
        /**
         * Render the entire case clause on a single line
         */
        RENDER_LINEAR,
        /**
         * If the then position is THEN_AFTER_WHEN_ALIGNED than this phase is used to determine the then position
         */
        DETERMINE_THEN_POSITION,
        /**
         * Just render
         */
        RENDER_NORMAL,
        /**
         * Render the then clause on its fallback position
         */
        RENDER_FALLBACK
    }

    private CaseType caseConfig;

    private RenderPhase renderPhase = RenderPhase.RENDER_NORMAL;

    private int thenPosition = 0;

    /**
     * Constructor
     *
     * @param config
     *            The effective configuration
     * @param context
     *            The parent context
     * @param caseConfig
     *            The CaseType config to use now. This can be the caseWhen or caseOperand config item
     */
    public CaseFormatContext(FormatConfiguration config, FormatContext context, CaseType caseConfig) {
        super(config, context);
        this.caseConfig = caseConfig;
    }

    /**
     * Copy onstructor
     *
     * @param original
     *            The CaseFormatContext to copy
     */
    public CaseFormatContext(CaseFormatContext original) {
        super(original);
        caseConfig = ConfigUtil.copy(original.caseConfig);
        renderPhase = original.renderPhase;
        thenPosition = original.thenPosition;
    }

    /**
     * Returns the case type to render an case clause or case statement.
     *
     * @return CaseType The caseOperand or caseWhen configuration item
     */
    public CaseType getCaseConfig() {
        return caseConfig;
    }

    /**
     * Returns the position at which THEN is to be rendered
     *
     * @return int the thenPosition
     */
    public int getThenPosition() {
        return thenPosition;
    }

    /**
     * Sets the position at which THEN is to be rendered
     *
     * @param thenPosition
     *            the position
     */
    public void setThenPosition(int thenPosition) {
        this.thenPosition = thenPosition;
    }

    /**
     * Sets the thenPosition if it is bigger
     *
     * @param thenPosition
     *            The position that is stored if bigger
     */
    public void maximizeThenPosition(int thenPosition) {
        if (this.thenPosition < thenPosition) {
            this.thenPosition = thenPosition;
        }
    }

    /**
     * Returns the current render phase
     *
     * @return RenderPhase The current phase
     */
    public RenderPhase getRenderPhase() {
        return renderPhase;
    }

    /**
     * Sets the current render phase
     *
     * @param renderPhase
     *            The phase to set
     */
    public void setRenderPhase(RenderPhase renderPhase) {
        this.renderPhase = renderPhase;
    }

    /**
     * @see FormatContext#equals(Object)
     */
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (!(otherObject instanceof CaseFormatContext)) {
            return false;
        }
        CaseFormatContext other = (CaseFormatContext) otherObject;
        if (this.thenPosition != other.thenPosition) {
            return false;
        }
        if (!ConfigUtil.equals(this.caseConfig, other.caseConfig)) {
            return false;
        }
        return super.equals(otherObject);
    }

    /**
     * @see FormatContext#clone()
     */
    public CaseFormatContext clone() {
        CaseFormatContext clone = (CaseFormatContext) super.clone();
        if (this.caseConfig != null) {
            clone.caseConfig = ConfigUtil.copy(this.caseConfig);
        }
        return clone;
    }
}
