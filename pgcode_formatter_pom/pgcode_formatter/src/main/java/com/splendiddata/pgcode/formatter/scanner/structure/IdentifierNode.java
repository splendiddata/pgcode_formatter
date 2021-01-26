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

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.LetterCaseType;
import com.splendiddata.pgcode.formatter.internal.*;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Just an identifier
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class IdentifierNode extends SrcNode implements Serializable, Comparable<IdentifierNode> {
    private static final long serialVersionUID = 1L;

    private final String identifier;

    private boolean notKeyword;

    /**
     * Constructor for subclasses
     *
     */
    protected IdentifierNode() {
        super(ScanResultType.IDENTIFIER, null);
        identifier = null;
    }

    /**
     * Constructor
     *
     * @param scanResult
     *            Source of the identifier
     */
    public IdentifierNode(ScanResult scanResult) {
        super(ScanResultType.IDENTIFIER, scanResult);
        if (scanResult == null) {
            identifier = "";
        } else {
            switch (scanResult.getType()) {
            case IDENTIFIER:
            case DOUBLE_QUOTED_IDENTIFIER:
                break;
            default:
                throw new AssertionError(
                        "An IdenfifierNode must be created from a scanResult of type IDENTIFIER, but is: "
                                + scanResult);

            }
            this.identifier = scanResult.getText();
        }
        setNext(scanResult.getNext());
    }

    /**
     * Constructor
     *
     * @param identifier
     *            Textual identifier
     */
    public IdentifierNode(String identifier) {
        super(ScanResultType.IDENTIFIER, null);
        this.identifier = identifier;
    }

    /**
     * @return String the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * If true, this identifier must not be interpreted as a keyword. This may be the case for example for column names
     * that look like a keyword but are just an identifier.
     *
     * @return boolean if true this is NOT a keyword. If yes, this MIGHT be a keyword.
     */
    public boolean isNotKeyword() {
        return notKeyword;
    }

    /**
     * Of notKeyword is true, then this keyword will NOT be interpreted as a keyword. If false, this MIGHT be
     * interpreted as a keyword.
     * <p>
     * This method is supposed to be invoked on identifiers that represent for example a column name.
     *
     * @param notKeyword
     *            true if this identifier should not be handled as keyword
     */
    public void setNotKeyword(boolean notKeyword) {
        this.notKeyword = notKeyword;
    }

    /**
     * @see java.lang.Object#toString()
     *
     * @return String the content
     */
    @Override
    public String toString() {
        return identifier;
    }

    /**
     * Compares this.getIdentifier() to other.getIdentifier()
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(IdentifierNode other) {
        return getIdentifier().compareTo(other.getIdentifier());
    }

    /**
     * Returns the hashCode of whatever comes from getIdentifier()
     *
     * @see java.lang.Object#hashCode()
     *
     * @return int hashCode
     */
    @Override
    public int hashCode() {
        return getIdentifier().hashCode();
    }

    /**
     * Returns true if the other node is an IdentifierNode and both getIdentifier() methods return equals results
     *
     * @see Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof IdentifierNode) {
            return getIdentifier().equals(((IdentifierNode) other).getIdentifier());
        }
        return false;
    }

    /**
     * @see ScanResult#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult, FormatConfiguration config) {
        return new RenderItem(pgBuiltInsToLetterCase(config), this, RenderItemType.IDENTIFIER);
    }

    /**
     * Converts the identifier/token to upper/lower case or keeps it unchanged based on the provided configuration.
     *
     * @param config
     *            {@link FormatConfiguration}
     * @return The converted identifier/token (or the identifier/token if unchanged).
     */
    private String pgBuiltInsToLetterCase(FormatConfiguration config) {
        if (isNotKeyword() || (LetterCaseType.UNCHANGED.equals(config.getLetterCaseKeywords())
                && LetterCaseType.UNCHANGED.equals(config.getLetterCaseFunctions()))) {
            return toString();
        }

        if (Dicts.pgPlPgsqlKeywords.contains(identifier.toUpperCase())) {
            switch (config.getLetterCaseKeywords()) {
            case LOWERCASE:
                return identifier.toLowerCase();
            case UPPERCASE:
                return identifier.toUpperCase();
            default:
                return identifier;
            }
        }
        else if (Dicts.pgFunctions.contains(identifier.toUpperCase())) {
            switch (config.getLetterCaseFunctions()) {
            case LOWERCASE:
                return identifier.toLowerCase();
            case UPPERCASE:
                return identifier.toUpperCase();
            default:
                return identifier;
            }
        }
        else {
            return identifier;
        }
    }
}
