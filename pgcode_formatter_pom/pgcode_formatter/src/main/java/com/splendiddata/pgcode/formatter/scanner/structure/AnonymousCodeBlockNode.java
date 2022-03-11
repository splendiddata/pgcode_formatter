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

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.internal.Util;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Code block using DO to execute an anonymous block DO [ LANGUAGE lang_name ] code This class represents the code.
 */
public class AnonymousCodeBlockNode extends SrcNode {
    SrcNode language;

    /**
     * Constructor
     * 
     * @param firstNode
     *            The first not of an anonymous code block, i.e. 'DO'
     */
    public AnonymousCodeBlockNode(ScanResult firstNode) {
        super(ScanResultType.FUNCTION_AS, firstNode);
        ScanResult priorNode = firstNode;
        ScanResult currentNode = firstNode.getNext();
        while (!currentNode.getType().isInterpretable() && !currentNode.isEof()) {
            priorNode = currentNode;
            currentNode = currentNode.getNext();
        }

        FunctionDefinitionNode functionDefinitionNode = new FunctionDefinitionNode(currentNode);
        priorNode.setNext(functionDefinitionNode);
        priorNode = functionDefinitionNode;
        currentNode = functionDefinitionNode.getNext();

        if (currentNode != null) {
            setNext(currentNode);
            priorNode.setNext(null);
        } else {
            setNext(null);
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

    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult, FormatConfiguration config) {
        RenderMultiLines result = new RenderMultiLines(this, formatContext, parentResult);

        ScanResult current = getStartScanResult();
        SrcNode srcNode = Util.interpretStatement(current);
        result.addRenderResult(srcNode.beautify(formatContext, result, config), formatContext);
        result.addWhiteSpaceIfApplicable();
        current = current.getNext();
        while (current != null && !current.isEof()) {
            srcNode = Util.interpretStatement(current);
            RenderResult renderResult = srcNode.beautify(formatContext, result, config);
            if (RenderItemType.WHITESPACE.equals(renderResult.getRenderItemType())) {
                result.addWhiteSpaceIfApplicable();
            } else {
                result.addRenderResult(renderResult, formatContext);
            }
            current = current.getNext();
        }
        result.removeTrailingSpaces();

        return result;
    }
}
