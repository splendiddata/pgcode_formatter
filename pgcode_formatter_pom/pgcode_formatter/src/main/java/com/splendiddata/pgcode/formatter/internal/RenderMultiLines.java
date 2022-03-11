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

package com.splendiddata.pgcode.formatter.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.scanner.ScanResult;

/**
 * Class for rendering result. It consists of one or more lines. A line is a string ended by a line separator string or
 * without a line separator string when it is the last one.
 * 
 * @author Splendid Data Product Development B.V.
 * @since 0.1
 */
public class RenderMultiLines implements RenderResult {
    private static final Logger log = LogManager.getLogger(RenderMultiLines.class);

    /**
     * Pattern to get trailing spaces in a separate group. After:
     * 
     * <pre>
     * Matcher m = TRAILING_SPACES_PATTERN.matcher(some_line);
     * m.matches();
     * </pre>
     * <ul>
     * <li>m.group(1) will contain the line without trailing spaces (which may be an empty string)</li>
     * <li>m.group(2) will contain the trailing spaces (or an empty string if there were'nt any)</li>
     * </ul>
     */
    private static final Pattern TRAILING_SPACES_PATTERN = Pattern.compile("^(.*?)(\\s*)$");
    private static final Pattern LEADING_SPACES_PATTERN = Pattern.compile("^(\\s*)(\\S.*)?$");
    private static final Pattern FIRST_LAST_LINES_SEPARATE_PATTERN = Pattern.compile("^([^\\n]*)\\n(.*?)([^\\n]*)$",
            Pattern.DOTALL);
    private static final Pattern LAST_LINE_SEPARATE_PATTERN = Pattern.compile("^(.*?)\\n([^\\n]*)$", Pattern.DOTALL);

    private int indent;
    private int indentBase;

    private final RenderMultiLines parentResult;

    private StringBuilder buffer;
    private StringBuilder lastLine;
    private int height;
    private int width;
    private int preserveLineFeedPosition;
    private int previousEolPosition = -1;;

    /**
     * Constructor.
     * 
     * @param scanResult
     *            The start scanResult. It is the start node of the statement/string that has to be rendered in this
     *            RenderMultiLines result. It can be null.
     * @param context
     *            The FormatContext that will be used
     * @param parentResult
     *            The RenderMultiLines to which this result will be added eventually. This may help getting the
     *            indenting right when a block of code needs to be aligned to itself. <br>
     *            May be null.
     */
    public RenderMultiLines(ScanResult scanResult, FormatContext context, RenderMultiLines parentResult) {
        this.parentResult = parentResult;
        lastLine = new StringBuilder(200);
        if (parentResult != null) {
            indentBase = parentResult.indentBase;
            indent = parentResult.indent;
        }
    }

    /**
     * @see java.lang.Object#clone()
     *
     * @return RenderMultiLines The cloned RenderMultiLines.
     */
    public RenderMultiLines clone() {
        try {
            RenderMultiLines clone = (RenderMultiLines) super.clone();
            if (this.buffer != null) {
                clone.buffer = new StringBuilder(this.buffer);
            }
            clone.lastLine = new StringBuilder(this.lastLine);
            return clone;
        } catch (CloneNotSupportedException e) {
            log.error("clone()", e);
            throw new RuntimeException("Clone after all not supported by RenderMultiLines", e);
        }
    }

    /**
     * Adds a child result to its parent render result. Every {@link RenderMultiLines} consists of one more
     * {@link RenderResult} which can be a RenderItem or a RenderMultiLines child. This way the render results of an sql
     * statement are nested (this depends of course on the format configuration).
     *
     * @param toAdd
     *            The element that has to be added to the multi lines result.
     * @param formatContext
     *            A FormatContext.
     * @return RenderMultiLines this
     * @throws IllegalStateException
     *             when invoked after invocation of {@link #beautify()}
     */
    public RenderMultiLines addRenderResult(RenderResult toAdd, FormatContext formatContext) {
        if (lastLine == null) {
            throw new IllegalStateException("addRenderResult() invoked after beautify()");
        }
        if (toAdd == null) {
            return this;
        }
        if (height == 0) {
            height = 1;
        }
        if (width < toAdd.getWidth()) {
            width = toAdd.getWidth();
        }
        String resultToAdd = toAdd.beautify();
        if (resultToAdd.isBlank()) {
            /*
             * When the RenderResult toAdd only consists of whitespace, then a single space character is added to the
             * current line if it didn't already end in a space character.
             */
            if (lastLine.length() > 0 && lastLine.charAt(lastLine.length() - 1) != ' ') {
                lastLine.append(' ');
            }
        } else {
            previousEolPosition = -1;
            if (toAdd.getHeight() <= 1) {
                lastLine.append(resultToAdd);
            } else {
                height += toAdd.getHeight() - 1;
                Matcher m = FIRST_LAST_LINES_SEPARATE_PATTERN.matcher(resultToAdd);
                if (m.matches()) {
                    lastLine.append(m.group(1));
                    if (lastLine.length() > width) {
                        width = lastLine.length();
                    }
                    if (buffer == null) {
                        buffer = new StringBuilder();
                    } else {
                        buffer.append('\n');
                    }
                    buffer.append(lastLine).append('\n').append(m.group(2));
                    buffer.setLength(buffer.length() - 1); // Remove the last linefeed
                    lastLine.setLength(0);
                    lastLine.append(m.group(3));
                    if (lastLine.length() == 0) {
                        indentLastLine();
                    }
                }
            }
        }
        return this;
    }

    public RenderMultiLines addEolComment(String text) {
        if (lastLine == null) {
            throw new IllegalStateException("addRenderResult() invoked after beautify()");
        }
        if (previousEolPosition >= 0 && lastLine.toString().isBlank()) {
            lastLine.setLength(0);
            lastLine.append(Util.nSpaces(previousEolPosition));
            previousEolPosition = -1;
        }
        int eolPosition = getPosition();
        lastLine.append(text);
        preserveLineFeedPosition = -1;
        addLine();
        previousEolPosition = eolPosition;
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
        int parentPosition = 0;
        if (height <= 1 && parentResult != null) {
            parentPosition = parentResult.getPosition();
        }
        if (lastLine == null) {
            Matcher m = LAST_LINE_SEPARATE_PATTERN.matcher(buffer);
            if (m.matches()) {
                return parentPosition + m.group(2).length();
            }
            return parentPosition + buffer.length();
        }
        return parentPosition + lastLine.length();
    }

    /**
     * Add a new line RenderItem (with indentation) to the render result. If a new line already exists, then it will be
     * replaced by a new line with indentation.
     * 
     * @param indentation
     *            The spaces to use as indentation.
     */
    public void addLine(String indentation) {
        if (lastLine == null) {
            throw new IllegalStateException("addLine(String) invoked after beautify()");
        }
        addLine();
        lastLine.setLength(0);
        lastLine.append(indentation);
    }

    /**
     * Add a new line and indents it
     * 
     * @return RenderMultiLines this
     */
    public RenderMultiLines addLine() {
        if (lastLine == null) {
            throw new IllegalStateException("addLine() invoked after beautify()");
        }
        if (buffer == null) {
            buffer = new StringBuilder();
        } else if (previousEolPosition >= 0) {
            // A newline character was already added because of end-of-line comment
            previousEolPosition = -1;
        } else {
            buffer.append('\n');
        }
        /*
         * Remove trailing spaces
         */
        Matcher m = TRAILING_SPACES_PATTERN.matcher(lastLine);
        m.matches();
        String line = m.group(1);
        buffer.append(line);
        if (preserveLineFeedPosition < 0) {
            preserveLineFeedPosition = buffer.length();
        }
        if (line.length() > width) {
            width = line.length();
        }

        lastLine.setLength(0);
        indentLastLine();
        if (height == 0) {
            // Nothing has been added yet
            height = 2;
        } else {
            height++;
        }
        return this;
    }

    /**
     * Adds spaces to lastLine to indent it
     */
    private void indentLastLine() {
        assert lastLine.length() == 0 : "The lastLine is supposed to be empty at indentLastLine(), but is <" + lastLine
                + ">";
        lastLine.append(Util.nSpaces(getTotalIndent()));
    }

    /**
     * Just adds a new line RenderItem to the render result.
     */
    public void addExtraLine() {
        if (lastLine == null) {
            throw new IllegalStateException("addExtraLine() invoked after beautify()");
        }
        if (buffer == null) {
            buffer = new StringBuilder();
        }
        buffer.append(lastLine);
        lastLine.setLength(0);
        height++;
    }

    /**
     * Just adds a whitespace RenderItem to this RenderMultiLines result.
     */
    public void addWhiteSpace() {
        if (lastLine == null) {
            throw new IllegalStateException("addWhiteSpace() invoked after beautify()");
        }
        lastLine.append(' ');
    }

    /**
     * Add a whitespace RenderItem to this RenderMultiLines result in case its last element is not a whitespace and it
     * is not a line feed. White space will not be added when renderResults list is empty.
     */
    public void addWhiteSpaceIfApplicable() {
        if (lastLine == null) {
            throw new IllegalStateException("addWhiteSpaceIfApplicable() invoked after beautify()");
        }
        if (lastLine.length() == 0 || !lastLine.substring(lastLine.length() - 1).isBlank()) {
            lastLine.append(' ');
        }
    }

    /**
     * @see RenderResult#isLastNonWhiteSpaceEqualToLinefeed()
     */
    @Override
    public boolean isLastNonWhiteSpaceEqualToLinefeed() {
        if (lastLine.toString().isBlank()) {
            if (buffer == null && parentResult != null) {
                return parentResult.isLastNonWhiteSpaceEqualToLinefeed();
            }
            return true;
        }
        return false;
    }

    /**
     * Removes last white space RenderItem elements from renderResults list.
     */
    public void removeTrailingSpaces() {
        if (lastLine == null) {
            throw new IllegalStateException("removeTrailingSpaces() invoked after beautify()");
        }
        Matcher m = TRAILING_SPACES_PATTERN.matcher(lastLine);
        m.matches();
        lastLine.setLength(lastLine.length() - m.group(2).length());
    }

    /**
     * Removes all trailing whitespace and possibly line feeds at the end of the content.
     * <p>
     * This might be handy to place a semi-colon directly after the last word in a statement instead of leaving it
     * dangling.
     * <p>
     * When a preserveLinefeedPosition is hit (the end of end-of-line comment), then the carret position will be on an
     * empty but indented line.
     *
     * @return RenderMultiLines this
     */
    public RenderMultiLines positionAfterLastNonWhitespace() {
        if (lastLine == null) {
            throw new IllegalStateException("positionAfterLastNonWhitespace() invoked after beautify()");
        }
        Matcher m = TRAILING_SPACES_PATTERN.matcher(lastLine);
        if (m.matches() && m.group(1).length() > 0) {
            lastLine.setLength(m.group(1).length());
        } else if (buffer != null) {
            if (preserveLineFeedPosition < -1) {
                preserveLineFeedPosition = buffer.length();
            }
            if (preserveLineFeedPosition < buffer.length()) {
                height--;
                m = LAST_LINE_SEPARATE_PATTERN.matcher(buffer);
                while (m.matches() && m.group(1).length() > preserveLineFeedPosition && m.group(2).isBlank()) {
                    height--;
                    buffer.setLength(m.group(1).length());
                    m = LAST_LINE_SEPARATE_PATTERN.matcher(buffer);
                }
                lastLine.setLength(0);
                if (m.matches()) {
                    lastLine.append(m.group(2));
                    buffer.setLength(m.group(1).length());
                } else {
                    lastLine = buffer;
                    buffer = null;
                }
            }
        } else {
            lastLine.setLength(0);
        }
        return this;
    }

    /**
     * Returns the number of lines in the RenderMultiLines result. A line is a string ended by a line separator string
     * or without a line separator string when it is the last one.
     * 
     * @return The height of the RenderMultiLines result.
     */
    @Override
    public int getHeight() {
        return height;
    }

    /**
     * In a RenderMultiLines result, it returns the width (including indentation) of the longest line.
     * 
     * @return The width of a RenderMultiLines result.
     */
    @Override
    public int getWidth() {
        if (lastLine != null) {
            Matcher m = TRAILING_SPACES_PATTERN.matcher(lastLine);
            m.matches();
            int actualWidth = m.group(1).length();
            if (actualWidth > width) {
                return actualWidth;
            }
        }
        return width;
    }

    /**
     * Returns the width of the first line of a RenderMultiLines result.
     * 
     * @return The width of the first line of a RenderMultiLines result.
     */
    @Override
    public int getWidthFirstLine() {
        if (buffer == null) {
            Matcher m = TRAILING_SPACES_PATTERN.matcher(lastLine);
            m.matches();
            return m.group(1).length();
        }
        int widthFirstLine = buffer.indexOf("\n");
        if (widthFirstLine < 0) {
            return buffer.length();
        }
        return widthFirstLine;
    }

    /**
     * A string representation of the beautified code.
     * 
     * @return A string representation of the beautified code.
     */
    @Override
    public String toString() {
        if (buffer == null) {
            return lastLine.toString();
        }
        if (lastLine == null) {
            return buffer.toString();
        }
        return new StringBuffer().append(buffer).append('\n').append(lastLine).toString();
    }

    /**
     * A string representation of the beautified code.
     * 
     * @return A string representation of the beautified code.
     */
    @Override
    public String beautify() {
        if (lastLine == null) {
            // Already beautified
            if (buffer == null) {
                // That's weird. There is nothing in here.
                return "";
            }
            return buffer.toString();
        }
        Matcher m = TRAILING_SPACES_PATTERN.matcher(lastLine);
        lastLine = null;
        m.matches();
        if (buffer == null) {
            return m.group(1);
        }
        return buffer.append("\n").append(m.group(1)).toString();
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
        if (lastLine == null) {
            throw new IllegalStateException("positionAt(int) invoked after beautify()");
        }
        int currentParentPosition = 0;
        if (height <= 1 && parentResult != null) {
            currentParentPosition = parentResult.getPosition();
        }
        int currentPosition = currentParentPosition + lastLine.length();
        if (currentPosition > position) {
            RenderMultiLines res = this;
            while (res != null && currentPosition > position) {
                Matcher m = TRAILING_SPACES_PATTERN.matcher(res.lastLine);
                m.matches();
                res.lastLine.setLength(m.group(1).length());
                currentPosition = currentParentPosition + res.lastLine.length();
                if (buffer != null || res.lastLine.length() > 0) {
                    break;
                }
                res = res.parentResult;
                if (res != null) {
                    currentParentPosition = res.getPosition();
                }
            }
        }
        if (currentPosition < position) {
            lastLine.append(Util.nSpaces(position - currentPosition));
        } else if (currentPosition > position) {
            addLine(Util.nSpaces(position));
        }
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
            this.indent = 0;
        } else {
            this.indent = indent;
        }
        return this;
    }

    /**
     * Adds the specified indent to the current indent
     * 
     * @param indent
     *            the indent to add to the current indent setting
     * @return RenderMultiLines this
     */
    public RenderMultiLines addIndent(int indent) {
        this.indent += indent;
        if (this.indent <= 0) {
            this.indent = 0;
        }
        return this;
    }

    /**
     * Returns the amount of spaces which which this RenderMultilines will increase the indent
     *
     * @return int the standard indent
     */
    public int getLocalIndent() {
        return indent;
    }

    /**
     * Adds up the standardIndents of this RenderMultiLine and all its parents
     *
     * @return int the standardIndent of this RenderMultiLine plus the standardIndent of all parent RenderMultiLines
     */
    public int getTotalIndent() {
        int totalIndent = indentBase + indent;
        if (totalIndent < 0) {
            totalIndent = 0;
        }
        return totalIndent;
    }

    /**
     * Indicates that the last linefeed of this RenderMultiLines must not be removed.
     * <p>
     * This might be handy for end of line comment.
     * 
     * @return RenderMultiLines this
     * @since 0.3
     */
    public RenderMultiLines preserveLineFeed() {
        preserveLineFeedPosition = -1;
        return this;
    }

    /**
     * Returns the position in the line relative to which indents will be performed - the first non-space character
     * position in the line.
     *
     * @return int the base indent
     * @since 0.3
     */
    public int getIndentBase() {
        return indentBase;
    }

    /**
     * Overwrites the indentBase
     *
     * @param indentBase
     *            The new indent base
     * @return RenderMultiLines this
     * @since 0.3
     */
    public RenderMultiLines setIndentBase(int indentBase) {
        this.indentBase = indentBase;
        return this;
    }
}
