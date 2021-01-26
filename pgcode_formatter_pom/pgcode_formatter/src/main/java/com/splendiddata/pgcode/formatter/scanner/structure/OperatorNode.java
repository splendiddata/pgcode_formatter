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

import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * A node containing a string that complies to the Postgres operator rules
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class OperatorNode extends SrcNode {
    private static final Logger log = LogManager.getLogger(OperatorNode.class);

    public  static final Pattern OPERATOR_CHARACTER_PATTERN = Pattern.compile("^[+\\-*/<>=~!@#%\\^\\&|`?]+$");
    
    private final String operator;

    /**
     * Constructor for a single-character operator
     *
     * @param scanResult
     *            Source of this node
     */
    public OperatorNode(ScanResult scanResult) {
        super(ScanResultType.OPERATOR, scanResult);
        this.operator = scanResult.getText();
        log.debug(()->this);
    }

    /**
     * Constructor for a multi-character operator
     *
     * @param operator
     *            The text of the operator
     * @param startNode
     *            The ScanResult that follows this
     */
    public OperatorNode(String operator, ScanResult startNode) {
        super(ScanResultType.OPERATOR, startNode);

        ScanResult nextNode = startNode;
        ScanResult prev = startNode;
        for (int i = 0; i < operator.length(); i++) {
            prev = nextNode;
            nextNode = nextNode.getNext();
        }
        prev.setNext(null);

        this.operator = operator;
        this.setNext(nextNode);
        log.debug(()->this);
    }

    /**
     * @see Object#toString()
     *
     * @return String the operator as string
     */
    @Override
    public String toString() {
        return operator;
    }

    /**
     * @see SrcNode#getText()
     *
     * @return String the operator as string
     */
    @Override
    public String getText() {
        return operator;
    }

    /**
     * @see SrcNode#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult, FormatConfiguration config) {
        RenderResult result = new RenderItem(toString(), this, RenderItemType.OPERATOR);

        return result;
    }
    
}
