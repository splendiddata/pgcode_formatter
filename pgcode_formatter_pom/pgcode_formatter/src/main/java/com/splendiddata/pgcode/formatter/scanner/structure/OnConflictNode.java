/*
 * Copyright (c) Splendid Data Product Development B.V. 2020 - 2022
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

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * The on conflict clause in an insert statement
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class OnConflictNode extends SrcNode implements WantsNewlineBefore {
    private int singleLineLength;

    /**
     * Constructor
     *
     * @param scanResult
     *            The word "on" in "on conflict ... "
     */
    public OnConflictNode(ScanResult scanResult) {
        super(ScanResultType.ON_CONFLICT_CLAUSE, scanResult);
        ScanResult previousNode = scanResult;
        ScanResult currentNode = scanResult;

        while (currentNode != null && !currentNode.isEof()) {

            if ("on".equalsIgnoreCase(currentNode.getText().toLowerCase())) {
                currentNode = currentNode.getNext();
            }

            if (currentNode.isStatementEnd()) {
                previousNode.setNext(null);
                setNext(currentNode);

                return;
            }

            switch (currentNode.getText().toLowerCase()) {
            case "conflict":
                SrcNode interpretedSrcNode = PostgresInputReader.toIdentifier(currentNode);
                previousNode.setNext(interpretedSrcNode);
                previousNode = interpretedSrcNode;
                currentNode = interpretedSrcNode.getNext();

                break;

            case "returning":
                previousNode.setNext(null);
                setNext(currentNode);
                return;
            case "(":
                interpretedSrcNode = PostgresInputReader.interpretStatementBody(currentNode);
                previousNode.setNext(interpretedSrcNode);
                previousNode = interpretedSrcNode;
                currentNode = interpretedSrcNode.getNext();

                break;
            default:
                switch (currentNode.getType()) {
                case IDENTIFIER:
                    currentNode.toString();
                    interpretedSrcNode = PostgresInputReader.interpretStatementBody(currentNode);
                    previousNode.setNext(interpretedSrcNode);
                    previousNode = interpretedSrcNode;
                    currentNode = interpretedSrcNode.getNext();
                    break;
                default:
                    previousNode = currentNode;
                    currentNode = currentNode.getNext();
                    break;
                }
            }
        }

        if (currentNode == null || currentNode.isEof()) {
            previousNode.setNext(null);
            setNext(currentNode);

            return;
        }
    }

    /**
     * @see java.lang.Object#toString()
     *
     * @return String formatted for debugging purposes
     */
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
    public RenderMultiLines beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        RenderMultiLines result = getCachedRenderResult(formatContext, parentResult, config);
        if (result != null) {
            return result;
        }

        int availableWidth = formatContext.getAvailableWidth();
        /*
         * Try to render on a single line
         */

        if (!config.getQueryConfig().isMajorKeywordsOnSeparateLine().booleanValue() && getSingleLineWidth(config) <= availableWidth) {
            result = new RenderMultiLines(this, formatContext, parentResult).addIndent(config.getStandardIndent());
            for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
                result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
            }
            if (result.getHeight() == 1 && result.getPosition() <= availableWidth) {
                return cacheRenderResult(result, formatContext, parentResult);
            }
        }

        /*
         * Single line didn't work out, so render multiline
         */
        result = new RenderMultiLines(this, formatContext, parentResult).addIndent(config.getStandardIndent());
        FormatContext contentContext = new FormatContext(config, formatContext)
                .setAvailableWidth(availableWidth - config.getStandardIndent());
        ScanResult node = getStartScanResult();
        result.addRenderResult(node.beautify(formatContext, result, config), formatContext);
        if (config.getQueryConfig().isMajorKeywordsOnSeparateLine().booleanValue()) {
            while (node != null
                    && !(node.is(ScanResultType.IDENTIFIER) && "conflict".equalsIgnoreCase(node.toString()))) {
                node = node.getNext();
                result.addRenderResult(node.beautify(contentContext, result, config), formatContext);
            }
            result.addLine();
        } else {
            result.addWhiteSpace();
        }
        for (node = node.getNext(); node != null; node = node.getNext()) {
            RenderResult itemResult = node.beautify(contentContext, result, config);
            if (node.is(ScanResultType.IDENTIFIER) && "do".equalsIgnoreCase(node.toString())) {
                result.addLine();
                result.addRenderResult(itemResult, formatContext);
            } else if (result.getPosition() > config.getStandardIndent()
                    && result.getPosition() + itemResult.getWidth() > availableWidth) {
                result.addLine();
                result.addRenderResult(node.beautify(contentContext, result, config), formatContext);
            } else {
                result.setIndent(result.getPosition());
                result.addRenderResult(itemResult, formatContext);
                result.setIndent(config.getStandardIndent());
            }
        }

        formatContext.setAvailableWidth(availableWidth);
        return cacheRenderResult(result, formatContext, parentResult);
    }

    /**
     * @see ScanResult#getSingleLineWidth(FormatConfiguration)
     */
    @Override
    public int getSingleLineWidth(FormatConfiguration config) {
        if (singleLineLength != 0) {
            /*
             * Been here before, so the answer can be given rapidly
             */
            return singleLineLength;
        }
        if (config.getQueryConfig().isMajorKeywordsOnSeparateLine().booleanValue()) {
            singleLineLength = -1;
            return singleLineLength;
        }
        int additionalLength;
        for (ScanResult node = this.getStartScanResult(); node != null; node = node.getNext()) {
            additionalLength = node.getSingleLineWidth(config);
            if (additionalLength < 0) {
                singleLineLength = -1;
                return singleLineLength;
            }
            singleLineLength += additionalLength;
        }
        return singleLineLength;
    }
}