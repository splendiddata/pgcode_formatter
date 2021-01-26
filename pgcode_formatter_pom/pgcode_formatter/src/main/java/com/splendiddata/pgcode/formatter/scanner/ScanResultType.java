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

package com.splendiddata.pgcode.formatter.scanner;

import com.splendiddata.pgcode.formatter.scanner.structure.InParentheses;

/**
 * Types of scanner results
 *
 * @author Splendid Data Product Development B.V.
 * @since 0.0.1
 */
public enum ScanResultType {
    /**
     * Text contains just whitespace
     */
    WHITESPACE(false),

    /**
     * The text contains an unquoted (key?)word
     */
    IDENTIFIER(true),

    /**
     * Combined identifier, like order by, group by ...etc
     */
    COMBINED_IDENTIFIER(true),

    /**
     * Probably an identifier, found between double quotes
     * <p>
     * Note that the double quotes are stripped off
     * </p>
     */
    DOUBLE_QUOTED_IDENTIFIER(true),

    /**
     * Just text found between single quotes
     * <p>
     * Note that the quotes are stripped off
     * </p>
     */
    LITERAL(true),

    /**
     * An escape string constant which is specified by writing the letter E (upper or lower case) just before the
     * opening single quote
     * <p>
     * Note that the E' and the ending quote are stripped off
     * </p>
     */
    ESCAPE_STRING(true),

    /**
     * Just a number of letters, numbers, underscores dollars that do not comply to the IDENTIFIER standard
     */
    WORD(true),

    /**
     * Single character
     */
    CHARACTER(true),

    /**
     * c-stype comment.
     * <p>
     * BEWARE! this might include newline character(s)
     * </p>
     */
    COMMENT(false),

    /**
     * End of line comment
     */
    COMMENT_LINE(false),

    /**
     * End o statement
     */
    SEMI_COLON(true),

    /**
     * A function call - identifier followed by an open parenthesis followed by comma separated arguments followed by a
     * closing parenthesis
     */
    FUNCTION_CALL(true),

    /**
     * New line
     */
    LINEFEED(false),

    /**
     * End of input.
     */
    EOF(true),

    /**
     * An error has occurred
     */
    ERROR(true),

    /**
     * Just text to be added to the output file
     */
    TEXT(true),

    /**
     * Type declaration
     */
    TYPE_DECLARATION(true),

    /**
     * Data type of a column or function parameter
     */
    DATA_TYPE(true),

    /**
     * Declaration of a function argument or a variable
     */
    DATA_DECLARATION(true),

    /**
     * The signature of a function or procedure. This includes the function name, parameters and return type
     */
    FUNCTION_SIGNATURE(true),

    /**
     * Definition of a function or a procedure
     */
    FUNCTION_DEFINITION(true),

    /**
     * The body of the function - from declare to end
     */
    FUNCTION_BODY(true),

    FUNCTION_AS(true),

    /**
     * A case clause
     */
    CASE_CLAUSE(true),

    /**
     * A case statement
     */
    CASE_STATEMENT(true),

    FOR_LOOP(true),

    /**
     * The when clause of a case statement or clause
     */
    WHEN_THEN_CLAUSE(true),

    SELECT_STATEMENT(true),

    INSERT_STATEMENT(true),

    /**
     * A "discovered data type" is a type that is taken for granted. It will represent the type of a field under a
     * data_name%type construct. Every field under ...%type will be taken for granted, and constructed on the fly.
     */
    DISCOVERED_DATA_TYPE(true),

    VALUE_LIST(true),
    ARGUMENT(true),

    FROM_ITEM_LIST(true),
    INTO_CLAUSE(true),
    GRANT_COMMAND(true),
    GRANT_ON(true),
    GRANT_PRIVILEGES(true),
    GRANT_TO_ROLES(true),
    GROUP_BY(true),
    HAVING_CLAUSE(true),
    ORDER_BY(true),
    PLPGSQL_LABEL(true),
    CURSOR_DECLARATION(true),

    PSQL_META_COMMAND(true),
    DOUBLE_BACKSLASH(true),

    /**
     * The limit clause of a select statement
     */
    LIMIT_CLAUSE(true),

    /**
     * a union clause including the following select statement
     */
    UNION_CLAUSE(true),

    /**
     * A string that complies to Postgres's operator rules - see <a href=
     * "https://www.postgresql.org/docs/current/sql-createoperator.html">https://www.postgresql.org/docs/current/sql-createoperator.html</a>
     */
    OPERATOR(true),

    /**
     * Fallback statement node. This is returned when a statement is expected but we cannot identify which - maybe
     * because we didn't implement that statement (yet).
     */
    JUST_A_STATEMENT(true),

    /**
     * A type cast in the form ::type
     */
    TYPE_CAST(true),

    /**
     * The on conflict clause of an insert statement
     */
    ON_CONFLICT_CLAUSE(true),

    /**
     * A declare section in a PLPGSQL code block
     */
    PLPGSQL_DECLARE_SECTION(true),

    /**
     * A PLpgSQL compound statement
     */
    PLPGSQL_BEGIN_END_BLOCK(true),

    /**
     * Just an opening parenthesis
     */
    OPENING_PARENTHESIS(true),

    /**
     * Just a closing parenthesis
     */
    CLOSING_PARENTHESIS(true),

    /**
     * Just a generic interpreted node
     */
    INTERPRETED(true),

    /**
     * A comma separated list
     */
    COMMA_SEPARATED_LIST(true), 
    
    /**
     * An {@link InParentheses}
     */
    IN_PARENTHESES(true);

    /**
     * Distinguishes text that needs to be interpreted from noise text like whitespace, comment etc.
     */
    private final boolean interpretable;

    /**
     * Constructor
     *
     * @param isInterpretable
     */
    private ScanResultType(boolean isInterpretable) {
        interpretable = isInterpretable;
    }

    /**
     * Distinguishes interpretable text from noise like whitespace, line feeds, comment etc.
     * 
     * @return boolean the interpretable
     */
    public final boolean isInterpretable() {
        return interpretable;
    }
}
