/*
 * Copyright (c) Splendid Data Product Development B.V. 2020 - 2021
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

import java.util.LinkedList;
import java.util.List;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.internal.Util;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Grant command. It consists of: GRANT GrantPrivilegesNode ON GrantOnNode TO GrantToRolesNode
 */
public class GrantCommandNode extends SrcNode {
    private final List<SrcNode> constituentParts = new LinkedList<>();
    SrcNode grantClause;
    GrantPrivilegesNode privileges;
    GrantOnNode grantOnObjects;
    GrantToRolesNode toRoles;

    /**
     * Constructor.
     * 
     * @param scanResult
     *            The node that contains "grant"
     */
    public GrantCommandNode(ScanResult scanResult) {
        super(ScanResultType.GRANT_COMMAND, scanResult);
        ScanResult currentNode = scanResult;

        ScanResult priorNode = null;
        if ("grant".equalsIgnoreCase(currentNode.getText().toLowerCase())) {
            grantClause = new IdentifierNode(currentNode);
            constituentParts.add(grantClause);

            priorNode = currentNode;
            currentNode = currentNode.getNext();

            if (currentNode == null) {
                return;
            }
        }

        if (priorNode != null) {
            priorNode.setNext(null);
        }

        privileges = new GrantPrivilegesNode(currentNode);
        currentNode = privileges.getNext();

        if (currentNode != null) {
            setNext(currentNode.getNext());
        } else {
            setNext(null);
            return;
        }

        if (currentNode.isStatementEnd()) {
            currentNode.setNext(null);
            privileges.setNext(null);
            return;
        }
        constituentParts.add(privileges);

        if ("on".equalsIgnoreCase(currentNode.getText().toLowerCase())) {
            grantOnObjects = new GrantOnNode(currentNode);
            currentNode = grantOnObjects.getNext();
            if (currentNode != null) {
                setNext(currentNode.getNext());
            } else {
                setNext(null);
                return;
            }
            if (currentNode.isStatementEnd()) {
                currentNode.setNext(null);
                toRoles.setNext(null);
                return;
            }
            constituentParts.add(grantOnObjects);
        }

        toRoles = new GrantToRolesNode(currentNode);
        constituentParts.add(toRoles);
        currentNode = toRoles.getNext();
        if (currentNode == null) {
            setNext(null);
            return;
        }
        if (currentNode.isStatementEnd()) {
            setNext(currentNode);
            currentNode.setNext(null);
            toRoles.setNext(null);
            return;
        }

        while (currentNode != null && !currentNode.isEof()) {
            if (currentNode.isStatementEnd()) {
                setNext(currentNode);

                return;
            }
            SrcNode srcNode = Util.interpretStatement(currentNode);
            constituentParts.add(srcNode);
            currentNode = srcNode.getNext();
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        for (SrcNode currentNode : constituentParts) {
            result.append(currentNode.toString());
        }
        return result.toString();
    }

    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult, FormatConfiguration config) {
        RenderMultiLines result = new RenderMultiLines(this, formatContext, parentResult);

        for (SrcNode currentNode : constituentParts) {
            RenderResult renderResult = currentNode.beautify(formatContext, result, config);
            result.addWhiteSpaceIfApplicable();
            result.addRenderResult(renderResult, formatContext);
        }

        return result;
    }
}
