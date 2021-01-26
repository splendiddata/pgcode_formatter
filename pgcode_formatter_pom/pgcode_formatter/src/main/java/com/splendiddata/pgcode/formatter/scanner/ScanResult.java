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

package com.splendiddata.pgcode.formatter.scanner;

import com.splendiddata.pgcode.formatter.CodeFormatterThreadLocal;
import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.internal.Util;
import com.splendiddata.pgcode.formatter.scanner.structure.CommentLineNode;
import com.splendiddata.pgcode.formatter.scanner.structure.CommentNode;
import com.splendiddata.pgcode.formatter.util.Msg;

/**
 * Interface for the result from the scanner.
 * 
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public interface ScanResult {

    /**
     * @return ScanResult the next
     */
    ScanResult getNext();

    /**
     * @param next
     *            the next to set
     */
    void setNext(ScanResult next);

    /**
     * @return String the text
     */
    String getText();

    /**
     * @return ScanResultType the type
     */
    ScanResultType getType();

    /**
     * Returns the parenthesisLevel AFTER the current word.
     *
     * @return int the parenthesisLevel
     */
    int getParenthesisLevel();

    /**
     * Returns the beginEndLevel AFTER the current word.
     *
     * Remark: beginEndLevel represents the 'begin end' nesting level and will be incremented when words like 'begin',
     * 'if' and 'loop' are encountered. It will be decremented when 'end' is encountered. In some cases, like start of a
     * transactions "begin;" or "begin transaction;", beginEndLevel will incremented but 'may be' never decremented
     * because the transaction will be committed using "commit;" instead of "end transaction". As long as beginEndLevel
     * is used relatively, there will be no issue even if the value of it is incorrect.
     *
     * @return int the beginEndLevel
     */
    int getBeginEndLevel();

    /**
     * Sets the begin end nesting level
     * @param level The begin end nesting level to set
     */
    default void setBeginEndLevel(int level) {
        // empty
    };

    /**
     * Identifies this ScanResult as the end of the statement. It may be one of ScanResultType SEMI_COLON or EOF.
     *
     * @return boolean true it this ScanResult demarks the end of the statement
     */
    boolean isStatementEnd();

    /**
     * Returns true if this ScanResult demarks the end of the input
     *
     * @return boolean true if this is the EOF mark
     */
    boolean isEof();

    /**
     * @return Msg the errorMessage
     */
    default Msg getErrorMessage() {
        return null;
    }

    /**
     * Convenience method to check the type
     *
     * @param type
     *            The type to check
     * @return boolean getType().equals(type)
     */
    default boolean is(ScanResultType type) {
        return getType().equals(type);
    }

    /**
     * The default implementation of the function beautify
     * 
     * @param formatContext
     *            A {@link FormatContext}
     * @param parentResult
     *            The RenderMultiLines to which the result of this beautify action will be added. May be null
     * @param config
     *            A {@link FormatConfiguration}
     * @return A beautified render result.
     */
    default RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        /*
         * C-style comment and end-of-line comment may have been ignored during interpretation. But they need to be
         * rendered properly. So let's do the interpretation after all.
         */
        if (ScanResultType.COMMENT.equals(getType()) && !(this instanceof CommentNode)) {
            return new CommentNode(this).beautify(formatContext, parentResult, config);
        }
        if (ScanResultType.COMMENT_LINE.equals(getType()) && !(this instanceof CommentLineNode)) {
            return new CommentLineNode(this).beautify(formatContext, parentResult, config);
        }

        RenderResult renderResult = new RenderItem(toString(), this,
                Util.convertScanResultTypeToRenderItemType(getType()));

        return renderResult;
    }

    /**
     * Checks whether a scanResult is a defined additional statement end.
     * 
     * @param scanResult
     *            A {@link ScanResult}
     * @return true when scanResult is a statement end.
     */
    default boolean isAdditionalStatementEnd(ScanResult scanResult) {
        if (CodeFormatterThreadLocal.isAdditionalStatementEnd(scanResult.getText())) {
            return true;
        }

        return false;
    }

    /**
     * Locates the ScanResult prior to the next interpretable ScanResult.
     * <p>
     * Many times it is nice to be able to just skip nodes that do not need interpretation like whitespace or comment.
     * But these nodes need to be preserved in the next pointer chain, and the following node that probably will be
     * interpreted should become part of the next pointer chain in most cases. So it is necessary to keep track of the
     * node preceding the one that is to be replaced with its interpreted node, so that the interpreted node can be
     * placed into the next pointer of that preceding node.
     * <p>
     * The locateNextInterpretable method returns the node preceding the node that is liable to be interpreted. The
     * returned node can be 'this'.
     *
     * @return ScanResult The node of which the next pointer points to the following interpretable ScanResult
     */
    default ScanResult locatePriorToNextInterpretable() {
        ScanResult priorNode;
        ScanResult currentNode;
        for (priorNode = this;; priorNode = currentNode) {
            currentNode = priorNode.getNext();
            if (currentNode == null || currentNode.getType().isInterpretable()) {
                return priorNode;
            }
            if (currentNode.is(ScanResultType.COMMENT_LINE) && !(currentNode instanceof CommentLineNode)) {
                currentNode = new CommentLineNode(currentNode);
                priorNode.setNext(currentNode);
            } else if (currentNode.is(ScanResultType.COMMENT) && !(currentNode instanceof CommentNode)) {
                currentNode = new CommentNode(currentNode);
                priorNode.setNext(currentNode);
            }
        }
    }

    /**
     * Checks whether the encountered line feed is followed by a line comment.
     * 
     * We distinguish between line comments at the end of line (i.e. after some source code) and line comments on a new
     * line (may be preceded by spaces or tabs).
     * When a line feed is encountered, a check is done whether it is followed by a line comment.
     * Based on this a decision can be made whether the line feed will be added to the formatted result or not.
     *
     * @return true if a line feed is followed by a line comment, otherwise false.
     */
    default boolean isMandatoryLineFeed() {
        boolean mandatoryLineFeed = false;
        if (ScanResultType.LINEFEED.equals(getType())) {
            ScanResult nextNonWhitespace;
            for (nextNonWhitespace = getNext(); nextNonWhitespace != null
                    && (nextNonWhitespace.is(ScanResultType.WHITESPACE)); nextNonWhitespace = nextNonWhitespace
                            .getNext()) {
                // Skip the whitespace
            }
            if (nextNonWhitespace != null && ScanResultType.COMMENT_LINE.equals(nextNonWhitespace.getType())) {
                mandatoryLineFeed = true;
            }
        }

        return mandatoryLineFeed;
    }

    /**
     * Returns the next interpretable node or null if there isn't any
     *
     * @return ScanResult the next interpretable scan result or null
     */
    default ScanResult getNextInterpretable() {
        for (ScanResult node = getNext(); node != null; node = node.getNext()) {
            if (node.getType().isInterpretable()) {
                return node;
            }
        }
        return null;
    }

    /**
     * @return ScanResult the next ScanResult that is not a whitespace or a line feed
     */
    default ScanResult getNextNonWhitespace() {
        ScanResult nextNonWhitespace;
        for (nextNonWhitespace = getNext(); nextNonWhitespace != null
                && (nextNonWhitespace.is(ScanResultType.WHITESPACE)
                        || nextNonWhitespace.is(ScanResultType.LINEFEED)); nextNonWhitespace = nextNonWhitespace
                                .getNext()) {
            // just skip the whitespace
        }

        return nextNonWhitespace;
    }
}
