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

package com.splendiddata.pgcode.formatter;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splendiddata.pgcode.formatter.configuration.xml.v1_0.TabsOrSpacesType;
import com.splendiddata.pgcode.formatter.internal.Util;

/**
 * Utility class with some straight-forward mapping
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public class CodeFormatter {
    public static final Logger log = LogManager.getLogger(CodeFormatter.class);

    /**
     * Utility class - no constructor
     *
     * @throws UnsupportedOperationException
     *             in all cases
     */
    private CodeFormatter() {
        throw new UnsupportedOperationException();
    }

    /**
     * Turns the inFile into a stream of formatted statements in String format.
     * <p>
     * Every result will end in a newline character. So if a statements has trailing comment, then that comment will be
     * with the statement in one result. Empty lines and lines that only contain comment between statements are returned
     * as results of their own.
     *
     * @param inFile
     *            The Reader that will provide the input
     * @param config
     *            The FormatConfiguration that tells how to format
     * @return Stream&lt;String&gt; The output, statement by statement, and with newlines and comment between statements
     *         as separate Strings
     * @throws IOException
     *             when the inFile Reader feels a need to do so
     */
    public static Stream<String> toStringResults(Reader inFile, FormatConfiguration config) throws IOException {
        /*
         * If all groups of spaces are to be replaced with tabs then the tabSplitPattern will be created. It will split
         * up every line in chunks of tab-width characters so that later trailing spaces in each chunk can be replaces
         * with a single tab character
         */
        Pattern tabSplitPattern = TabsOrSpacesType.TABS.equals(config.getTabs().getTabsOrSpaces())
                ? Pattern.compile("(\n)|([^\n]{1," + config.getTabs().getTabWidth() + "})")
                : null;
        Pattern tabReplacementPattern = tabSplitPattern == null ? null : Pattern.compile("\\s{2,}$");

        /*
         * If only the indent is to be replaced by tabs, then the leadingSpacesPattern will be filled to help replacing
         * leading spaces by tabs
         */
        Pattern leadingSpacesPattern = tabSplitPattern == null
                && TabsOrSpacesType.TABS.equals(config.getIndent().getTabsOrSpaces())
                        ? Pattern.compile("\\n([\\s^\\n]+)")
                        : null;

        /*
         * now get some work done.
         */
        return Util.toRenderResults(inFile, config).map(renderResult -> {
            String result = renderResult.beautify();

            if (tabSplitPattern != null) {
                result = Util.replaceSpacesByTabs(config, tabSplitPattern, tabReplacementPattern, result);
            } else if (leadingSpacesPattern != null) {
                result = Util.replaceLeadingSpaces(config, leadingSpacesPattern, result);
            }
            return result;
        });
    }
}
