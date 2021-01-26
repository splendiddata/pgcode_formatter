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

import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;
import com.splendiddata.pgcode.formatter.util.Msg;

/**
 * An error from the source code
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class ErrorNode extends SrcNode {
    public static final String ERROR_INDICATOR = ">>> ";

    private final Msg errorMessage;

    private SrcNode erroneousNode;

    /**
     * Constructor
     *
     * @param scanResult
     *            The error that appeared while scanning the source file
     */
    public ErrorNode(ScanResult scanResult) {
        super(ScanResultType.ERROR, scanResult);
        this.errorMessage = scanResult.getErrorMessage();
    }

    /**
     * Constructor
     *
     * @param msg
     *            The error message for this error node
     */
    public ErrorNode(Msg msg) {
        super(ScanResultType.ERROR, null);
        this.errorMessage = msg;
    }

    /**
     * Constructor
     *
     * @param scanResult
     *            The scan result that caused toe error
     * @param msg
     *            Message explaining the error
     */
    public ErrorNode(ScanResult scanResult, Msg msg) {
        super(ScanResultType.ERROR, scanResult);
        this.errorMessage = msg;
    }

    /**
     * @return Msg the errorMessage
     */
    public final Msg getErrorMessage() {
        return errorMessage;
    }

    /**
     * @return SrcNode the erroneousNode
     */
    public final SrcNode getErroneousNode() {
        return erroneousNode;
    }

    /**
     * @param erroneousNode
     *            the erroneousNode to set
     * @return ErrorNode this
     */
    public final ErrorNode setErroneousNode(SrcNode erroneousNode) {
        this.erroneousNode = erroneousNode;
        return this;
    }

    @Override
    public String toString() {
        return errorMessage.toString();
    }

}
