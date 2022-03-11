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
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.PlpgsqlDeclareConstantPositionType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.PlpgsqlDeclareSectionType;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.RelativePositionTypeEnum;
import com.splendiddata.pgcode.formatter.internal.FormatContext;
import com.splendiddata.pgcode.formatter.internal.PostgresInputReader;
import com.splendiddata.pgcode.formatter.internal.RenderItem;
import com.splendiddata.pgcode.formatter.internal.RenderItemType;
import com.splendiddata.pgcode.formatter.internal.RenderMultiLines;
import com.splendiddata.pgcode.formatter.internal.RenderResult;
import com.splendiddata.pgcode.formatter.internal.Util;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;
import com.splendiddata.pgcode.formatter.scanner.ScanResultType;

/**
 * Represents the entire declare section of a PLpgSQl code block
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class PlpgsqlDeclareSection extends SrcNode implements WantsNewlineBefore {
    private static final int CONSTANT_LENGTH = "constant ".length();

    /**
     * Constructor
     *
     * @param scanResult
     *            The word "DELCARE"
     */
    public PlpgsqlDeclareSection(ScanResult scanResult) {
        super(ScanResultType.PLPGSQL_DECLARE_SECTION, PostgresInputReader.toIdentifier(scanResult));
        assert "declare".equalsIgnoreCase(scanResult
                .toString()) : "A PlpgsqlDeclareSection should start with the word DECLARE, not with " + scanResult;

        ScanResult lastInterpreted = getStartScanResult();
        ScanResult priorNode = lastInterpreted;
        for (ScanResult currentNode = priorNode
                .getNext(); currentNode != null
                        && !currentNode.isEof()
                        && !(currentNode.is(ScanResultType.IDENTIFIER)
                                && "begin".equalsIgnoreCase(currentNode.toString())); currentNode = priorNode
                                        .getNext()) {
            currentNode = PostgresInputReader.interpretPlpgsqlStatementStart(currentNode);
            switch (currentNode.getType()) {
            case WHITESPACE:
            case LINEFEED:
                break;
            default:
                lastInterpreted = currentNode;
                break;
            }
            priorNode.setNext(currentNode);
            priorNode = currentNode;
        }
        setNext(lastInterpreted.getNext());
        lastInterpreted.setNext(null);
    }

    /**
     * @see ScanResult#beautify(FormatContext, RenderMultiLines, FormatConfiguration)
     */
    @Override
    public RenderResult beautify(FormatContext formatContext, RenderMultiLines parentResult,
            FormatConfiguration config) {
        int parentPosition = 0;
        if (parentResult != null) {
            if (!parentResult.isLastNonWhiteSpaceEqualToLinefeed()) {
                parentResult.addLine();
            }
            parentPosition = parentResult.getPosition();
        }
        /*
         * Determine the offset of the data type
         */
        PlpgsqlDeclareSectionType declareSectionConfig = config.getLanguagePlpgsql().getDeclareSection();
        int dataTypePosition = declareSectionConfig.getDataTypePosition().getMinPosition().intValue();
        boolean determineDataTypeStartPosition = RelativePositionTypeEnum.VERTICALLY_ALIGNED
                .equals(declareSectionConfig.getDataTypePosition().getAlignment())
                && !declareSectionConfig.getDataTypePosition().getMinPosition()
                        .equals(declareSectionConfig.getDataTypePosition().getMaxPosition());
        ScanResult declaration = getStartScanResult();
        for (declaration = declaration.getNextInterpretable(); declaration != null; declaration = declaration
                .getNextInterpretable()) {
            if (declaration instanceof SrcNode) {
                ScanResult node = ((SrcNode) declaration).getStartScanResult();
                if (node instanceof IdentifierNode) {
                    ((IdentifierNode) node).setNotKeyword(true);
                    if (determineDataTypeStartPosition) {
                        int length = config.getStandardIndent() + node.toString().length() + 1; // plus one for a space after the variable name
                        if (PlpgsqlDeclareConstantPositionType.ALIGNED_BEFORE_DATA_TYPE
                                .equals(declareSectionConfig.getDataTypePosition().getConstantPosition())) {
                            node = node.getNextInterpretable();
                            if (node instanceof IdentifierNode && "constant".equalsIgnoreCase(node.toString())) {
                                length += CONSTANT_LENGTH;
                            }
                        }
                        if (length > dataTypePosition) {
                            dataTypePosition = length;
                        }
                    }
                }
            }
        }
        if (dataTypePosition > declareSectionConfig.getDataTypePosition().getMaxPosition().intValue()) {
            dataTypePosition = declareSectionConfig.getDataTypePosition().getMaxPosition().intValue();
        }

        /*
         * Now start rendering
         */
        RenderMultiLines result = new RenderMultiLines(this, formatContext, parentResult).setIndentBase(parentPosition)
                .setIndent(config.getStandardIndent());
        declaration = getStartScanResult();
        result.addRenderResult(declaration.beautify(formatContext, result, config), formatContext); // The variable name

        for (declaration = declaration.getNext(); declaration != null; declaration = declaration.getNext()) {
            if (declaration.getType().isInterpretable()) {
                if (!result.isLastNonWhiteSpaceEqualToLinefeed()) {
                    result.addLine();
                }
                if (RelativePositionTypeEnum.SUBSEQUENT
                        .equals(declareSectionConfig.getDataTypePosition().getAlignment())
                        || !(declaration instanceof SrcNode)) {
                    RenderResult declarationResult = declaration.beautify(formatContext, result, config);
                    result.addRenderResult(declarationResult, formatContext);
                } else {
                    RenderMultiLines declarationResult = new RenderMultiLines(declaration, formatContext, parentResult);
                    ScanResult node = ((SrcNode) declaration).getStartScanResult();
                    declarationResult.addRenderResult(node.beautify(formatContext, result, config), formatContext); // the variable name
                    for (node = node.getNextNonWhitespace(); node != null
                            && !node.getType().isInterpretable(); node = node.getNextNonWhitespace()) {
                        // copy comment if any
                        declarationResult.addRenderResult(new RenderItem("", RenderItemType.WHITESPACE), formatContext);
                        declarationResult.addRenderResult(node.beautify(formatContext, declarationResult, config),
                                formatContext);
                    }
                    int toPosition = dataTypePosition - config.getStandardIndent() + parentPosition;
                    if (node instanceof IdentifierNode && "constant".equalsIgnoreCase(node.toString())
                            && PlpgsqlDeclareConstantPositionType.ALIGNED_BEFORE_DATA_TYPE
                                    .equals(declareSectionConfig.getDataTypePosition().getConstantPosition())) {
                        toPosition -= CONSTANT_LENGTH;
                    }
                    declarationResult.positionAt(toPosition);
                    Util.renderStraightForward(node, declarationResult, formatContext, config);
                    result.addRenderResult(declarationResult, formatContext);
                }
            } else {
                if (declaration.is(ScanResultType.LINEFEED)) {
                    result.addLine();
                } else {
                    if (declaration.getType().isInterpretable() || result.isLastNonWhiteSpaceEqualToLinefeed()) {
                        result.positionAt(result.getIndentBase() + config.getStandardIndent());
                    }
                    result.addRenderResult(declaration.beautify(formatContext, result, config), formatContext);
                }
            }
        }
        result.removeTrailingSpaces();

        return result;
    }

    /**
     * @see com.splendiddata.pgcode.formatter.scanner.structure.SrcNode#getText()
     *
     * @return String the entire original text of the declare section
     */
    @Override
    public String getText() {
        StringBuilder result = new StringBuilder();
        for (ScanResult node = getStartScanResult(); node != null; node = node.getNext()) {
            result.append(node);
        }
        return result.toString();
    }

    /**
     * @see java.lang.Object#toString()
     *
     * @return String the entire original text of the declare section
     */
    @Override
    public String toString() {
        return getText();
    }

    /**
     * @see ScanResult#getSingleLineWidth(FormatConfiguration)
     *
     * @return -1 as we will never render an entire declare section on a single line
     */
    @Override
    public int getSingleLineWidth(FormatConfiguration config) {
        return -1;
    }

}
