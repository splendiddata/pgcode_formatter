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

import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A PL/PGSQL &lt;&lt;label&gt;&gt;
 * 
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class PlpgsqlLabel extends SrcNode implements WantsNewlineBefore {

    /**
     * Constructor
     * <p>
     * Please invoke via {@link #from(ScanResult)}
     *
     * @param start
     *            The ScanResult containing the first &lt; sign of the &lt;&lt;label&gt;&gt;
     */
    private PlpgsqlLabel(ScanResult start) {
        super(ScanResultType.PLPGSQL_LABEL, start);
    }

    /**
     * Checks if the node and the following two can be interpreted as a PLpgSQL label.
     * <p>
     * If the node already is a PlpgsqlLabel, then that will be returned.
     * <p>
     * If the node is a "&lt;&lt;" operator, followed by an identifier, followed by a "&gt;&gt;" operator node, then a
     * new PlpgsqlLabel object will be constructed and returned.
     * <p>
     * In all other cases the return will be null.
     * 
     * @param startNode
     *            The scan result that might start a label
     * @return PlpgsqlLabel if the node is a "&lt;&lt;" operator, followed by and identifier, followed by a "&gt;&gt;"
     *         operator or null if it isn't
     */
    public static PlpgsqlLabel from(ScanResult startNode) {
        if (startNode instanceof PlpgsqlLabel) {
            return (PlpgsqlLabel) startNode;
        }
        if (startNode == null) {
            return null;
        }
        ScanResult node = startNode;
        ScanResult priorNode = startNode;
        if (node.is(ScanResultType.CHARACTER) && "<".equals(node.toString())) {
            node = node.getNext();
            if (node == null || !(node.is(ScanResultType.CHARACTER) && "<".equals(node.toString()))) {
                return null;
            }
            priorNode = node.locatePriorToNextInterpretable();
        } else if (node.is(ScanResultType.OPERATOR) && "<<".contentEquals(node.toString())) {
            priorNode = node.locatePriorToNextInterpretable();
        } else {
            return null;
        }
        node = priorNode.getNext();
        if (node == null || !(node.is(ScanResultType.IDENTIFIER) || node.is(ScanResultType.DOUBLE_QUOTED_IDENTIFIER))) {
            return null;
        }
        node = PostgresInputReader.interpretIdentifier(node);
        priorNode.setNext(node);
        if ( !(node instanceof IdentifierNode) || node instanceof QualifiedIdentifierNode) {
            return null;
        }
        priorNode = node.locatePriorToNextInterpretable();
        node = priorNode.getNext();
        if (node == null) {
            return null;
        }
        if (node.is(ScanResultType.CHARACTER) && ">".equals(node.toString())) {
            node = node.getNext();
            if (node == null || !(node.is(ScanResultType.CHARACTER) && ">".equals(node.toString()))) {
                return null;
            }
            priorNode = node;
            node = node.getNext();
            priorNode.setNext(null);
        } else if (node.is(ScanResultType.OPERATOR) && ">>".contentEquals(node.toString())) {
            priorNode = node;
            node = node.getNext();
            priorNode.setNext(null);
        } else {
            return null;
        }
        PlpgsqlLabel result = new PlpgsqlLabel(startNode);
        result.setNext(node);
        return result;
    }

    /**
     * @see com.splendiddata.pgcode.formatter.scanner.structure.SrcNode#getText()
     */
    @Override
    public String getText() {
        return toString();
    }

}
