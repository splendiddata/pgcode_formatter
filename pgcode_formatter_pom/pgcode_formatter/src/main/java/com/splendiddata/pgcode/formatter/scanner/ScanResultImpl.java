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

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.util.Msg;

/**
 * Result from the scanner
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class ScanResultImpl implements ScanResult {
    private static final Logger log = LogManager.getLogger(ScanResult.class);

    private final ScanResultType type;
    private final String text;
    private final Msg errorMessage;
    private final int parenthesisLevel;
    private int beginEndLevel;
    private SourceScanner scanner;
    private ScanResult next;

    /**
     * Constructor
     *
     * @param type
     *            Type of the scan result
     * @param text
     *            Content of the scan result
     * @param scanner
     *            The scanner that delivered this scan result
     */
    public ScanResultImpl(ScanResultType type, String text, SourceScanner scanner) {
        super();
        this.type = type;
        this.text = text;
        this.errorMessage = null;
        this.scanner = scanner;
        this.parenthesisLevel = scanner.getParenthesisNestingLevel();
        this.beginEndLevel = scanner.getBeginEndNestingLevel();
    }

    /**
     * Constructor
     *
     * @param errorMessage
     *            The message to be shown
     * @param scanner
     *            The scanner that delivered this scan result
     */
    public ScanResultImpl(Msg errorMessage, SourceScanner scanner) {
        this.type = ScanResultType.ERROR;
        this.text = "";
        this.errorMessage = errorMessage;
        this.scanner = scanner;
        this.parenthesisLevel = scanner.getParenthesisNestingLevel();
        this.beginEndLevel = scanner.getBeginEndNestingLevel();
    }

    /**
     * @return ScanResultType the type
     */
    public final ScanResultType getType() {
        return type;
    }

    /**
     * @return String the text
     */
    public final String getText() {
        return text;
    }

    /**
     * @return Msg the errorMessage
     */
    public final Msg getErrorMessage() {
        return errorMessage;
    }

    /**
     * @return ScanResult the next
     */
    public final ScanResult getNext() {
        if (next == null && scanner != null && !ScanResultType.EOF.equals(type)) {
            try {
                next = scanner.scan();
                scanner = null;
            } catch (IOException e) {
                log.error("getNext()->failed", e);
            }
        }
        return next;
    }

    /**
     * @param next
     *            the next to set
     */
    public final void setNext(ScanResult next) {
        this.next = next;
    }

    /**
     * Returns the parenthesisLevel AFTER the current word.
     * 
     * @return int the parenthesisLevel
     */
    public final int getParenthesisLevel() {
        return parenthesisLevel;
    }

    /**
     * Returns the beginEndLevel AFTER the current word.
     * 
     * @return int the beginEndLevel
     */
    public final int getBeginEndLevel() {
        return beginEndLevel;
    }

    /**
     * Sets the begin end nesting level
     * @param level The begin end nesting level to set
     */
    public final void setBeginEndLevel(int level) {
        beginEndLevel = level;
    }

    /**
     * Identifies this ScanResult as the end of the statement. It may be one of ScanResultType SEMI_COLON, EOF or an
     * additional end of statement.
     *
     * @return boolean true it this ScanResult demarks the end of the statement
     */
    public boolean isStatementEnd() {
        return isStandardStatementEnd() || isAdditionalStatementEnd(this);
    }

    /**
     * Identifies this ScanResult as the end of the statement. It may be one of ScanResultType SEMI_COLON or EOF
     *
     * @return boolean true it this ScanResult represents the end of the statement
     */
    public boolean isStandardStatementEnd() {
        switch (type) {
        case SEMI_COLON:
        case EOF:
            return true;
        default:
            return false;
        }
    }

    /**
     * Returns the content of this ScanResult as String for debugging purposes
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String result = text;
        if (ScanResultType.LITERAL.equals(type)) {
            result = '\'' + text + '\'';
        } else if (ScanResultType.DOUBLE_QUOTED_IDENTIFIER.equals(type)) {
            result = '"' + text + '"';
        }

        return result;
    }

    /**
     * Returns true if this ScanResult demarks the end of the input
     *
     * @return boolean true if this is the EOF mark
     */
    public boolean isEof() {
        return ScanResultType.EOF.equals(type);
    }

}
