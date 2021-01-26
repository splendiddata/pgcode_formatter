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

package com.splendiddata.pgcode.formatter.internal;

import java.util.LinkedList;
import java.util.ListIterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.FormatConfiguration;
import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.TabsOrSpacesType;
import com.splendiddata.pgcode.formatter.scanner.ScanResult;

/**
 * Class for rendering result. It consists of one or more lines. A line is a string ended by a line separator string or
 * without a line separator string when it is the last one.
 */
public class RenderMultiLines implements RenderResult {
    private static final Logger log = LogManager.getLogger(RenderMultiLines.class);
    private LinkedList<RenderResult> renderResults = new LinkedList<>();
    private ScanResult startScanResult;

    private int standardIndent = FormatContext.indent(true).length();
    private boolean overrideFirstIndent;

    /**
     * Constructor.
     * 
     * @param scanResult
     *            The start scanResult. It is the start node of the statement/string that has to be rendered in this
     *            RenderMultiLines result. It can be null.
     * @param context
     *            The FormatContext that will be used
     */
    public RenderMultiLines(ScanResult scanResult, FormatContext context) {
        this.startScanResult = scanResult;
    }

    /**
     * @see java.lang.Object#clone()
     *
     * @return RenderMultiLines The cloned RenderMultiLines.
     */
    public RenderMultiLines clone() {
        try {
            RenderMultiLines clone = (RenderMultiLines) super.clone();
            clone.renderResults = new LinkedList<>();
            for (RenderResult renderResult : renderResults) {
                clone.renderResults.add(renderResult.clone());
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            log.error("clone()", e);
            throw new RuntimeException("Clone after all not supported by RenderMultiLines", e);
        }
    }

    @Override
    public ScanResult getStartScanResult() {
        return startScanResult;
    }

    /**
     * Returns the renderResults.
     * 
     * @return The renderResults.
     */
    public LinkedList<RenderResult> getRenderResults() {
        return renderResults;
    }

    /**
     * Appends the specified element to the end of this list.
     * 
     * @param e
     *            The element to be appended to this list.
     */
    public void putRenderResult(RenderResult e) {
        if (e == null) {
            throw new IllegalArgumentException("putRenderResult(e=null)->e is not allowed to be null");
        }
        log.trace(() -> "putRenderResult(e=" + e + ")");
        renderResults.add(e);
    }

    /**
     * Appends the specified element to the end of the renderResults list if is not a linefeed.
     * 
     * @param e
     *            The element to be appended to the renderResults list.
     */
    private void addToResults(RenderResult e) {
        if (e != null) {
            if (RenderItemType.WHITESPACE.equals(e.getRenderItemType())
                    || RenderItemType.LINEFEED.equals(e.getRenderItemType())) {
                this.addWhiteSpaceIfApplicable();
            } else {
                renderResults.add(e);
            }
        }
    }

    /**
     * Adds a child result to its parent render result. Every {@link RenderMultiLines} consists of one more
     * {@link RenderResult} which can be a RenderItem or a RenderMultiLines child. This way the render results of an sql
     * statement are nested (this depends of course on the format configuration).
     *
     * @param e
     *            The element that has to be added to the multi lines result.
     * @param formatContext
     *            A FormatContext.
     * @return RenderMultiLines this
     */
    public RenderMultiLines addRenderResult(RenderResult e, FormatContext formatContext) {
        if (e != null) {
            RenderResult toAdd = e;
            if (toAdd instanceof RenderMultiLines) {
                ((RenderMultiLines) toAdd).indent(standardIndent, formatContext.getConfig());
                addToResults(toAdd);
            } else {
                if (!(toAdd instanceof RenderItem && ((RenderItem) toAdd).getNonBreakableText().length() == 0)) {
                    if (RenderItemType.LINEFEED.equals(toAdd.getRenderItemType()) && toAdd.getStartScanResult() != null
                            && toAdd.getStartScanResult().isMandatoryLineFeed()) {
                            addLine();
                    } else {
                        addToResults(toAdd);
                    }
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(new StringBuilder().append("addRenderResult, indent=").append(standardIndent).append(", caller=")
                    .append(Thread.currentThread().getStackTrace()[2]).append(" =\n").append(this.beautify()));
        }
        return this;
    }

    /**
     * Returns the width of the last line of render result, i.e. the 'text' (including spaces and indentation) after the
     * last new line character.
     *
     * @return The position/column of the last character (new line character excluded) of the complete text in the
     *         render result.
     */
    public int getPosition() {
        int result = 0;
        RenderResult last;
        LinkedList<RenderItem> renderItems = getRenderItems();
        ListIterator<RenderItem> listIterator = renderItems.listIterator(renderItems.size());
        while (listIterator.hasPrevious()) {
            last = listIterator.previous();
            if (RenderItemType.LINEFEED.equals(last.getRenderItemType())) {
                return result;
            } else {
                result += last.getWidth();
            }
        }
        return result;
    }

    /**
     * Add a new line RenderItem (with indentation) to the render result. If a new line already exists, then it will be
     * replaced by a new line with indentation.
     * 
     * @param indentation
     *            The spaces to use as indentation.
     */
    public void addLine(String indentation) {
        if (renderResults.isEmpty()) {
            putRenderResult(new RenderItem(Util.LF, RenderItemType.LINEFEED));
            putRenderResult(new RenderItem(indentation, RenderItemType.WHITESPACE));
        }
        if (isLastNonWhiteSpaceEqualToLinefeed()) {
            RenderResult last = getLast();
            if (RenderItemType.WHITESPACE.equals(last.getRenderItemType())) {
                ((RenderItem) last).setNonBreakableText(indentation);
            } else {
                putRenderResult(new RenderItem(indentation, RenderItemType.WHITESPACE));
            }
        } else {
            removeTrailingSpaces();
            putRenderResult(new RenderItem(Util.LF, RenderItemType.LINEFEED));
            putRenderResult(new RenderItem(indentation, RenderItemType.WHITESPACE));
        }
    }

    /**
     * Add a new line RenderItem to the render result only if the last RenderItem is not a new line.
     */
    public void addLine() {
        if (renderResults.isEmpty()) {
            return;
        }
        if (isLastNonWhiteSpaceEqualToLinefeed()) {
            RenderResult last = getLast();
            if (RenderItemType.WHITESPACE.equals(last.getRenderItemType())) {
                ((RenderItem) last).setNonBreakableText(Util.nSpaces(standardIndent));
            } else {
                putRenderResult(new RenderItem(Util.nSpaces(standardIndent), RenderItemType.WHITESPACE));
            }
        } else {
            removeTrailingSpaces();
            putRenderResult(new RenderItem(Util.LF, RenderItemType.LINEFEED));
            putRenderResult(new RenderItem(Util.nSpaces(standardIndent), RenderItemType.WHITESPACE));
        }
    }

    /**
     * Just adds a new line RenderItem to the render result.
     */
    public void addExtraLine() {
        putRenderResult(new RenderItem(Util.LF, RenderItemType.LINEFEED));
    }

    /**
     * Adds a new line RenderItem at the beginning of the render result
     * 
     * @param lineExists
     *            Indicates that a line feed already exists in the result
     * @return RenderResult this.
     */
    public RenderResult addLineAtStart(boolean lineExists) {
        if (!lineExists) {
            if (Util.isNullOrEmpty(this.renderResults)) {
                putRenderResult(new RenderItem(Util.LF, RenderItemType.LINEFEED));
            } else if (!RenderItemType.LINEFEED.equals(this.renderResults.getFirst().getRenderItemType())) {
                this.renderResults.addFirst(new RenderItem(Util.LF, RenderItemType.LINEFEED));
            }
        }

        return this;
    }

    /**
     * It replaces the last white space or line feed {@link RenderItem} element of the RenderMultiLines result by the
     * provided element. When the last element is not of type {@link RenderItem}, it searches recursively until the last
     * {@link RenderItem} element is found.
     * 
     * @param e
     *            The new element that will replace the last element.
     */
    public void replaceLast(RenderResult e) {
        RenderResult last = renderResults.peekLast();
        if (last != null) {
            if (last instanceof RenderMultiLines) {
                LinkedList<RenderItem> renderItems = last.getRenderItems();
                RenderItem renderItem = renderItems.peekLast();
                if (renderItem != null && (RenderItemType.LINEFEED.equals(renderItem.getRenderItemType()))
                        || RenderItemType.WHITESPACE.equals(last.getRenderItemType())) {
                    renderItems.removeLast();
                    renderResults.add(e);
                }
            } else {
                if (RenderItemType.LINEFEED.equals(last.getRenderItemType())
                        || RenderItemType.WHITESPACE.equals(last.getRenderItemType())) {
                    renderResults.removeLast();
                    renderResults.add(e);
                }
            }
        }
    }

    /**
     * Just adds a whitespace RenderItem to this RenderMultiLines result.
     */
    public void addWhiteSpace() {
        renderResults.add(new RenderItem(Util.space, RenderItemType.WHITESPACE));
    }

    /**
     * Add a whitespace RenderItem to this RenderMultiLines result in case its last element is not a whitespace and it
     * is not a line feed. White space will not be added when renderResults list is empty.
     */
    public void addWhiteSpaceIfApplicable() {
        RenderResult last = getLast();

        if (last != null && !RenderItemType.LINEFEED.equals(last.getRenderItemType())
                && !RenderItemType.WHITESPACE.equals(last.getRenderItemType())) {
            this.addWhiteSpace();
        }
    }

    /**
     * Returns the last {@link RenderItem} element of this RenderMultiLines result.
     * 
     * @return the last {@link RenderItem} element or null if empty.
     */
    @Override
    public RenderResult getLast() {
        RenderResult last = this.renderResults.peekLast();
        if (log.isTraceEnabled()) {
            log.trace("getLast() -> first result = " + last);
        }
        if (last != null) {
            if (last instanceof RenderItem) {
                return last;
            }
            LinkedList<RenderItem> renderItems = last.getRenderItems();
            if (renderItems != null && !renderItems.isEmpty()) {
                last = renderItems.peekLast();
                if (log.isTraceEnabled()) {
                    log.trace("getLast() -> second result = " + last);
                }
            }
        }

        return last;
    }

    /**
     * @see RenderResult#getFirst()
     */
    @Override
    public RenderResult getFirst() {
        RenderResult first = this.renderResults.peekFirst();

        if (first != null) {
            LinkedList<RenderItem> renderItems = first.getRenderItems();
            first = renderItems.peekFirst();
        }

        return first;
    }

    /**
     * Removes the first element of renderResults
     */
    public void removeFirst() {
        this.renderResults.pollFirst();
    }

    /**
     * Removes the last element of renderResults
     */
    public void removeLast() {
        this.renderResults.pollLast();
    }

    /**
     * Returns the last non white space, non linefeed and not a comment RenderItem element from renderResults.
     * 
     * @return The last element from renderResults that is not of type LINEFEED, WHITESPACE, COMMENT or COMMENT_LINE
     */
    public RenderResult getLastNonWhiteSpace() {
        @SuppressWarnings("unchecked")
        LinkedList<RenderResult> renderResultsTemp = (LinkedList<RenderResult>) this.renderResults.clone();

        RenderResult last = renderResultsTemp.pollLast();
        while (last != null && (RenderItemType.LINEFEED.equals(last.getRenderItemType())
                || RenderItemType.WHITESPACE.equals(last.getRenderItemType())
                || RenderItemType.COMMENT.equals(last.getRenderItemType())
                || RenderItemType.COMMENT_LINE.equals(last.getRenderItemType()))) {
            last = renderResultsTemp.pollLast();
        }

        while (last != null && (last instanceof RenderMultiLines)) {
            last = ((RenderMultiLines) last).getLastNonWhiteSpace();
        }

        return last;
    }

    /**
     * @see RenderResult#isLastNonWhiteSpaceEqualToLinefeed()
     */
    @Override
    public boolean isLastNonWhiteSpaceEqualToLinefeed() {
        boolean result = false;

        RenderItem current;
        LinkedList<RenderItem> renderItems = getRenderItems();
        ListIterator<RenderItem> listIterator = renderItems.listIterator(renderItems.size());
        while (listIterator.hasPrevious()) {
            current = listIterator.previous();
            if (RenderItemType.LINEFEED.equals(current.getRenderItemType())) {
                return true;
            } else if (!RenderItemType.WHITESPACE.equals(current.getRenderItemType())) {
                return false;
            }
        }

        return result;
    }

    /**
     * Removes trailing line feed RenderItem elements from renderResults list.
     */
    public void removeTrailingLineFeeds() {
        RenderResult last = renderResults.peekLast();
        if (last != null) {
            if (last instanceof RenderMultiLines) {
                ((RenderMultiLines) last).removeTrailingLineFeeds();
            } else {
                boolean foundLinefeed = false;
                boolean foundSomethingElse = false;
                for (ListIterator<RenderResult> it = renderResults.listIterator(renderResults.size()); !foundLinefeed
                        && !foundSomethingElse && it.hasPrevious();) {
                    RenderResult item = it.previous();
                    if (RenderItemType.LINEFEED.equals(item.getRenderItemType())) {
                        foundLinefeed = true;
                    } else if (!RenderItemType.WHITESPACE.equals(item.getRenderItemType())) {
                        foundSomethingElse = true;
                    }
                }
                if (foundLinefeed) {
                    for (last = renderResults.peekLast(); RenderItemType.WHITESPACE.equals(last.getRenderItemType())
                            || RenderItemType.LINEFEED
                                    .equals(last.getRenderItemType()); last = renderResults.peekLast()) {
                        renderResults.removeLast();
                    }
                }
            }
        }

    }

    /**
     * Removes last white space RenderItem elements from renderResults list.
     */
    public void removeTrailingSpaces() {
        RenderResult last = renderResults.peekLast();

        if (last != null && RenderItemType.WHITESPACE.equals(last.getRenderItemType())) {
            renderResults.removeLast();
        }
    }

    /**
     * Removes the leading white spaces from this RenderMultiLines result.
     */
    public void removeLeadingSpaces() {
        RenderResult first = renderResults.peekFirst();

        while (first != null && RenderItemType.WHITESPACE.equals(first.getRenderItemType())) {
            renderResults.removeFirst();
            first = renderResults.peekFirst();
            if (first == null || !RenderItemType.WHITESPACE.equals(first.getRenderItemType())) {
                return;
            } else {
                removeLeadingSpaces();
            }
        }

    }

    /**
     * Returns the number of lines in the RenderMultiLines result. A line is a string ended by a line separator string
     * or without a line separator string when it is the last one.
     * 
     * @return The height of the RenderMultiLines result.
     */
    @Override
    public int getHeight() {
        int height = 0;
        if (!Util.isNullOrEmpty(renderResults)) {
            height++;
        }

        RenderItem current;
        LinkedList<RenderItem> renderItems = getRenderItems();
        ListIterator<RenderItem> listIterator = renderItems.listIterator();
        while (listIterator.hasNext()) {
            current = listIterator.next();
            if (RenderItemType.LINEFEED.equals(current.getRenderItemType())) {
                height++;
            } else if (current instanceof FunctionDefinitionRenderItem) {
                height += current.getHeight();
            }
        }

        return height;
    }

    /**
     * In a RenderMultiLines result, it returns the width (including indentation) of the longest line.
     * 
     * @return The width of a RenderMultiLines result.
     */
    @Override
    public int getWidth() {
        int temp = 0;
        int finalWidth = 0;

        RenderItem current;
        LinkedList<RenderItem> renderItems = getRenderItems();
        ListIterator<RenderItem> listIterator = renderItems.listIterator();
        while (listIterator.hasNext()) {
            current = listIterator.next();
            if (RenderItemType.LINEFEED.equals(current.getRenderItemType())) {
                finalWidth = Math.max(temp, finalWidth);
                // The indentation in a linefeed RenderItem should be added
                // to the width of the following line
                temp = 0;
            } else {
                temp += current.getWidth();
            }
        }

        finalWidth = Math.max(temp, finalWidth);
        return finalWidth;
    }

    /**
     * Returns the width of the first line of a RenderMultiLines result.
     * 
     * @return The width of the first line of a RenderMultiLines result.
     */
    @Override
    public int getWidthFirstLine() {
        int result = 0;

        RenderResult current;
        LinkedList<RenderItem> renderItems = getRenderItems();
        ListIterator<RenderItem> listIterator = renderItems.listIterator();
        while (listIterator.hasNext()) {
            current = listIterator.next();
            if (RenderItemType.LINEFEED.equals(current.getRenderItemType())) {
                return result;
            } else {
                result += current.getWidth();
            }
        }

        return result;
    }

    /**
     * Returns the width of the first RenderItem element from renderResults that is not a line feed and not a comment.
     *
     * @return The width of the first RenderItem element from renderResults that is not a line feed and not a comment.
     */
    public int getWidthFirstItem() {
        int result = 0;

        RenderResult current;
        LinkedList<RenderItem> renderItems = getRenderItems();
        ListIterator<RenderItem> listIterator = renderItems.listIterator();
        while (listIterator.hasNext()) {
            current = listIterator.next();
            if (!RenderItemType.LINEFEED.equals(current.getRenderItemType())
                    && !RenderItemType.COMMENT_LINE.equals(current.getRenderItemType())
                    && !RenderItemType.COMMENT.equals(current.getRenderItemType())) {
                return current.getWidth();
            }
        }

        return result;
    }

    /**
     * Returns the (nested) {@link RenderResult} constituents as list of {@link RenderItem} elements.
     *
     * @return A list of {@link RenderItem} elements.
     */
    public LinkedList<RenderItem> getRenderItems() {
        LinkedList<RenderItem> result = new LinkedList<>();
        RenderResult current;
        ListIterator<RenderResult> listIterator = this.renderResults.listIterator();
        while (listIterator.hasNext()) {
            current = listIterator.next();
            if (current instanceof RenderMultiLines) {
                LinkedList<RenderItem> renderItems = current.getRenderItems();
                result.addAll(renderItems);
            } else {
                result.add((RenderItem) current);
            }
        }

        return result;
    }

    /**
     * A string representation of the beautified code.
     * 
     * @return A string representation of the beautified code.
     */
    @Override
    public String toString() {
        // beautified string
        StringBuffer result = new StringBuffer();

        for (RenderResult renderResult : renderResults) {
            result.append(renderResult.beautify());
        }

        return result.toString();
    }

    /**
     * A string representation of the beautified code.
     * 
     * @return A string representation of the beautified code.
     */
    @Override
    public String beautify() {
        StringBuffer result = new StringBuffer();

        for (RenderResult renderResult : renderResults) {
            if (renderResult == null) {
                log.error("beautify(): renderResult is null in " + renderResults);
            } else {
                log.trace(() -> "beautify(): append: " + renderResult.beautify());
                result.append(renderResult.beautify());
            }
        }

        return result.toString();
    }

    /**
     * Indent this RenderResult using the indentation int. Or in other words: add a couple of spaces after each linefeed
     * 
     * @param indentation
     *            The number of spaces to use for indentation
     * @param config The format configuration
     * @return The indented RenderResult.
     */
    private RenderMultiLines indent(int indentation, FormatConfiguration config) {
        if (this.overrideFirstIndent) {
            overrideFirstIndent = false;
            return this;
        }
        if (indentation == 0 || this.renderResults.size() == 0) {
            return this;
        }

        for (ListIterator<RenderItem> it = getRenderItems().listIterator(); it.hasNext();) {
            RenderItem renderItem = it.next();
            if (RenderItemType.LINEFEED.equals(renderItem.getRenderItemType())) {
                if (it.hasNext()) {
                    renderItem = it.next();
                    if (RenderItemType.WHITESPACE.equals(renderItem.getRenderItemType())) {
                        renderItem.setNonBreakableText(
                                Util.nSpaces(indentation + renderItem.getNonBreakableText().length()));
                    } else {
                        it.previous();
                        it.add(new RenderItem(Util.nSpaces(indentation), RenderItemType.WHITESPACE));
                    }
                } else {
                    it.add(new RenderItem(Util.nSpaces(indentation), RenderItemType.WHITESPACE));
                }
            } else if (renderItem instanceof FunctionDefinitionRenderItem) {
                if (config.isIndentInnerFunction()) {
                    String nonBreakableText = renderItem.getNonBreakableText();
                    if (config != null && TabsOrSpacesType.TABS.equals(config.getIndent().getTabsOrSpaces())) {
                        Integer tabWidth = config.getTabs().getTabWidth();
                        nonBreakableText = nonBreakableText.replaceAll("\n",
                                "\n" + Util.nTabs(indentation / tabWidth) + Util.nSpaces(indentation % tabWidth));
                    } else {
                        nonBreakableText = nonBreakableText.replaceAll("\n", "\n" + Util.nSpaces(indentation));
                    }
                    renderItem.setNonBreakableText(nonBreakableText);
                }
                it.add(renderItem);
            }
        }

        return this;
    }

    /**
     * Just return null. A {@link RenderMultiLines} does not have a RenderItemType as it may contain elements from
     * different types.
     * 
     * @return null.
     */
    @Override
    public RenderItemType getRenderItemType() {
        return null;
    }

    /**
     * Positions the following output at the requested position in the line. First trailing spaces are removed. Then if
     * the current position is before the requested position, then enough spaces are added to get to the right position.
     * But if the current position is already at or beyond the requested position, then a linefeed will be inserted
     * followed by enough spaces to get to the requested position
     *
     * @param position
     *            The position in the line to "tab" to
     */
    public void positionAt(int position) {
        removeTrailingSpaces();
        int currentPosition = getPosition();
        if (position > 0) {
            if (currentPosition >= position) {
                addLine(Util.nSpaces(position));
            } else {
                renderResults.add(new RenderItem(Util.nSpaces(position - currentPosition), RenderItemType.WHITESPACE));
            }
        } else {
            if (currentPosition > 0) {
                addLine();
            }
        }
    }

    /**
     * Sets the indent that will be implemented on each added result.
     * 
     * @param standardIndent
     *            the standardIndent to set
     * @return RenderMultiLines this
     */
    public RenderMultiLines setIndent(String standardIndent) {
        this.standardIndent = standardIndent.length();
        return this;
    }

    /**
     * Sets the indent that will be implemented on each added result.
     * 
     * @param indent
     *            the standardIndent to set
     * @return RenderMultiLines this
     */
    public RenderMultiLines setIndent(int indent) {
        if (indent <= 0) {
            standardIndent = 0;
        } else {
            standardIndent = indent;
        }
        return this;
    }

    /**
     * Returns the amount of spaces which which this RenderMultilines will increase the indent
     *
     * @return int the standard indent
     */
    public int getStandardIndent() {
        return standardIndent;
    }

    /**
     * Asserts that the indentation as given to this object will not be altered when this RenderMultiLines is added into
     * the total result via the {@link #addRenderResult(RenderResult, FormatContext)} method.
     *
     * @return RenderMultiLines this
     */
    public RenderMultiLines setOverrideIndent() {
        this.overrideFirstIndent = true;
        return this;
    }
}
