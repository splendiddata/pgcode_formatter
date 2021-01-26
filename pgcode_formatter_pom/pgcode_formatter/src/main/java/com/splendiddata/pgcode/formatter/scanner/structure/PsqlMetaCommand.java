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

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A PSQL meta command starting by "\". Examples:
 * \set ON_ERROR_STOP ON
 * \set VERBOSITY default
 */
public class PsqlMetaCommand extends SrcNode {

    public PsqlMetaCommand(ScanResult scanResult) {
        super(ScanResultType.PSQL_META_COMMAND, scanResult);
        ScanResult previousNode = scanResult;
        ScanResult currentNode = scanResult.getNext();
        while (currentNode != null && !currentNode.isEof()) {

            if (currentNode.isStatementEnd()) {
                previousNode.setNext(null);
                setNext(currentNode);
                return;
            }

            switch (currentNode.getType()) {
            case CHARACTER:
                if ("\\".equals(currentNode.getText())) {
                    previousNode.setNext(null);
                    setNext(currentNode);
                    return;
                }
                previousNode = currentNode;
                currentNode = currentNode.getNext();
                break;
            case LINEFEED:
            case DOUBLE_BACKSLASH:
                previousNode.setNext(null);
                setNext(currentNode.getNext());
                return;
            default:
                previousNode = currentNode;
                currentNode = currentNode.getNext();
                break;
            }
        }

        if (currentNode == null || currentNode.isEof()) {
            previousNode.setNext(null);
            setNext(currentNode);
            return;
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        ScanResult current = getStartScanResult();
        while (current != null && !current.isEof()) {
            result.append(current.toString());
            current = current.getNext();
        }
        return result.toString();
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult, FormatConfiguration config) {
        RenderMultiLines result = new RenderMultiLines(this, formatContext);

        ScanResult current = getStartScanResult();
        while (current != null && !current.isEof()) {
            if ("end".equalsIgnoreCase(current.getText())) {
                result.addLine();
            }
            result.addRenderResult(current.beautify(formatContext, result, config), formatContext);
            if ("begin".equalsIgnoreCase(current.getText())) {
                result.addLine();
            }
            current = current.getNext();
        }

        return result;
    }
}
