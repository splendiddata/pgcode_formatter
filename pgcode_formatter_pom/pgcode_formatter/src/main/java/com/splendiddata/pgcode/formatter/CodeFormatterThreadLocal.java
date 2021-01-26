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

/**
 * Thread local for the code formatter.
 * To parse a sql statement, sometimes additional end of statement characters/strings
 * are needed to identify the end of a certain (sub)statement. For example the function
 * code delimiter.
 *
 */
public class CodeFormatterThreadLocal implements Runnable {

    private static final ThreadLocal<String> statementEnd = new ThreadLocal<String>();

    /**
     * Returns the value of statementEnd.
     * @return statementEnd as a string.
     */
    public static String getStatementEnd() {
        return statementEnd.get();
    }

    /**
     * Sets an additional end of statement string.
     * @param value
     */
    public static void setStatementEnd(String value) {
        statementEnd.set(value);
    }

    /**
     *
     * @param text
     * @return true if the provided string is an end of statement.
     */
    public static boolean isAdditionalStatementEnd(String text) {
        if (getStatementEnd() != null && getStatementEnd().equals(text)) {
            return true;
        }

        return false;
    }

    @Override
    public void run() {
    }
}