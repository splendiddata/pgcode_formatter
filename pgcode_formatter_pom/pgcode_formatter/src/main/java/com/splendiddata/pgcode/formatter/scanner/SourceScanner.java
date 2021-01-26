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

/**
 * Interface for the generated {@link com.splendiddata.pgcode.formatter.scanner.SourceScannerImpl} to avoid circular dependencies
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public interface SourceScanner {
    /**
     * Makes the next ScanResult available from the source file
     *
     * @return ScanResult A scan result
     * @throws IOException
     *             if anything goes wrong
     */
    ScanResult scan() throws IOException;

    /**
     * Closes the scanner's input reader
     *
     * @throws IOException
     *             if anything goes wrotn
     */
    void yyclose() throws IOException;

    /**
     * Returns the current parenthesis level. Every opening parenthesis increases the level, every closing parenthesis
     * decreases it
     *
     * @return int the parenthesis level after the last scanned ScanResult
     */
    int getParenthesisNestingLevel();

    /**
     * Returns the current beginEnd nesting level. Every "begin" identifier increases the level, every "end" identifier
     * decreases it
     *
     * @return int the parenthesis level after the last scanned ScanResult
     */
    int getBeginEndNestingLevel();
}
