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

import com.splendiddata.pgcode.formatter.util.Msg;
import com.splendiddata.pgcode.formatter.util.MsgKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Deque;
import java.util.ArrayDeque;

%%

%class SourceScannerImpl
%implements com.splendiddata.pgcode.formatter.scanner.SourceScanner
%function scan
%type ScanResult
%public
%char
%unicode

%{
    private static final Logger log = LogManager.getLogger(SourceScanner.class);

    private final StringBuilder text = new StringBuilder();
    private String dollarDollarQuote;

    private final Deque<Integer> stateStack = new ArrayDeque<>();

    private int parenthesisNestingLevel = 0;

    private int beginEndNestingLevel = 0;

    private boolean endActive = false;

    private static final int MAX_COMMENT_SIZE = 10000;
    private static final int MAX_COMMENT_ERROR_TEXT_SIZE = 300;

    @Override
    public int getParenthesisNestingLevel() {
        return parenthesisNestingLevel;
    }

    @Override
    public int getBeginEndNestingLevel() {
        return beginEndNestingLevel;
    }

    private void setState(int nextState) {
        stateStack.clear();
        yybegin(nextState);
    }

    private void pushState(int nextState) {
        stateStack.push(Integer.valueOf(yystate()));
        yybegin(nextState);
    }

    private void popState() {
        Integer nextState = stateStack.poll();
        if (nextState == null) {
            yybegin(YYINITIAL);
        } else {
            yybegin(nextState.intValue());
        }
    }

%}

/*
 * OK, here is a short description of lex/flex rules behavior.
 * The longest pattern which matches an input string is always chosen.
 * For equal-length patterns, the first occurring in the rules list is chosen.
 * INITIAL is the starting state, to which all non-conditional rules apply.
 * Exclusive states change parsing rules while the state is active.  When in
 * an exclusive state, only those rules defined for that state apply.
 *
 * We use exclusive states for quoted strings, extended comments,
 * and to eliminate parsing troubles for numeric strings.
 * Exclusive states:
 *  <xb> bit string literal
 *  <C_STYLE_COMMENT> extended C-style comments
 *  <DOUBLE_QUOTED_IDENTIFIER> delimited identifiers (double-quoted identifiers)
 *  <xh> hexadecimal numeric string
 *  <SINGLE_QUOTED_STRING> standard quoted strings
 *  <xe> extended quoted strings (support backslash escape sequences)
 *  <xui> quoted identifier with Unicode escapes
 *  <xuiend> end of a quoted identifier with Unicode escapes, UESCAPE can follow
 *  <xus> quoted string with Unicode escapes
 *  <xusend> end of a quoted string with Unicode escapes, UESCAPE can follow
 *  <xeu> Unicode surrogate pair in extended quoted string
 *
 * Remember to add an <<EOF>> case whenever you add a new exclusive state!
 * The default one is probably not the right thing.
 */

%xstate C_STYLE_COMMENT
%xstate DOUBLE_QUOTED_IDENTIFIER
%xstate SINGLE_QUOTED_STRING
%xstate ESCAPE_STRING
%xstate xdolq

spacechar =         [ \t\f\r]
space =             {spacechar}+
newline =           {spacechar}*[\r]*\n[\r]*
commentline =       (\-\-)[^\r\n]*[\r\n]{0,1}

quote =             '
quoted =            [^']+

doublequote =       \"
doublequoted =      [^\"\n]+

/* Quoted string that allows backslash escapes */
xestart =		    [eE]{quote}

xescquote =	        \\{quote}
xdquote =           {quote}{quote}
xeinside =	        [^']

double_backslash = \\\\

/* $foo$ style quotes ("dollar quoting")
 * The quoted string starts with $foo$ where "foo" is an optional string
 * in the form of an identifier, except that it may not contain "$",
 * and extends to the first occurrence of an identical string.
 * There is *no* processing of the quoted text.
 *
 * {dolqfailed} is an error rule to avoid scanner backup when {dolqdelim}
 * fails to match its trailing "$".
 */
dolq_start =	[A-Za-z\200-\377_]
dolq_cont =	[A-Za-z\200-\377_0-9]
dolqdelim =	\$({dolq_start}{dolq_cont}*)?\$
dolqinside =	[^$]+

xcstart =           \/\*
xcstop =            \*\/
xcinside =          [^*/]+

ident_start =       [A-Za-z\200-\377_]
ident_cont	=	    [A-Za-z\200-\377_0-9\$]

identifier =        {ident_start}{ident_cont}*

word =              [A-Za-z\200-\377_0-9][A-Za-z\200-\377_0-9\$]*|\$[A-Za-z\200-\377_0-9]*

semicolon =         ;
star =              \*
slash =             \/
openParenthesis =   \(
closeParenthesis =  \)
other =             .

%%

<YYINITIAL> {space} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<YYINITIAL> {space} :\"" + txt + "\"");
            }
            return new ScanResultImpl(ScanResultType.WHITESPACE, txt, this);
}

<YYINITIAL> {newline} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<YYINITIAL> {newline} :\"" + txt + "\"");
            }
            return new ScanResultImpl(ScanResultType.LINEFEED, txt, this);
}

<YYINITIAL> {commentline} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<YYINITIAL> {commentline} :\"" + txt + "\"");
            }
            return new ScanResultImpl(ScanResultType.COMMENT_LINE, txt, this);
}

<YYINITIAL> {xcstart} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<YYINITIAL> {xcstart} :\"" + txt + "\"");
            }
            text.setLength(0);
            text.append(txt);
            pushState(C_STYLE_COMMENT);
}

<YYINITIAL> {double_backslash} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<YYINITIAL> {double_backslash} :\"" + txt + "\"");
            }
            return new ScanResultImpl(ScanResultType.DOUBLE_BACKSLASH, txt, this);
}

<C_STYLE_COMMENT> {xcstart} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<C_STYLE_COMMENT> {xcstart} :\"" + txt + "\"");
            }
            text.append(txt);
            pushState(C_STYLE_COMMENT);
}

<C_STYLE_COMMENT> {xcstop} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<C_STYLE_COMMENT> {xcstop} :\"" + txt + "\"");
            }
            text.append(txt);
            popState();

            if (yystate() == YYINITIAL) {
                return new ScanResultImpl(ScanResultType.COMMENT, text.toString(), this);
            }
}

<C_STYLE_COMMENT> {xcinside}|{slash}|{star} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<C_STYLE_COMMENT> {xcinside}|{slash}|{star} :\"" + txt + "\"");
            }
            text.append(txt);
            if (text.length() > MAX_COMMENT_SIZE) {
                text.setLength(MAX_COMMENT_SIZE);
                text.append(" ...");
            }
}

<C_STYLE_COMMENT> <<EOF>> {
            log.trace("<C_STYLE_COMMENT> <<EOF>>");
            if (text.length() > MAX_COMMENT_ERROR_TEXT_SIZE) {
                text.setLength(MAX_COMMENT_ERROR_TEXT_SIZE);
                text.append(" ...");
            }
            popState();
            return new ScanResultImpl(new Msg(MsgKey.valueOf("msg.unterminated.slash.star.comment"), text.toString()), this);
}

<YYINITIAL> {quote} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<YYINITIAL> {quote} :\"" + txt + "\"");
            }
            text.setLength(0);
            pushState(SINGLE_QUOTED_STRING);
}

<YYINITIAL> {xestart} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<YYINITIAL> {xestart} :\"" + txt + "\"");
            }
            text.setLength(0);
            pushState(ESCAPE_STRING);
}

<ESCAPE_STRING> {xescquote} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<ESCAPE_STRING> {xescquote} :\"" + txt + "\"");
            }
            text.append(txt);
}

<ESCAPE_STRING> {xdquote} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<ESCAPE_STRING> {xdquote} :\"" + txt + "\"");
            }
            text.append(txt);
}

<ESCAPE_STRING> {xeinside}|{double_backslash} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<ESCAPE_STRING> {xeinside}|{double_backslash}:\"" + txt + "\"");
            }
            text.append(txt);
}

<ESCAPE_STRING> {quote} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<ESCAPE_STRING> {quote} :\"" + txt + "\"");
            }
            popState();
            return new ScanResultImpl(ScanResultType.ESCAPE_STRING, text.toString(), this);
}

<ESCAPE_STRING> <<EOF>> {
            log.trace("<ESCAPE_STRING> <<EOF>>");
            ScanResultImpl scanResult = new ScanResultImpl(ScanResultType.ESCAPE_STRING, text.toString(), this);
            scanResult.setNext(new ScanResultImpl(ScanResultType.EOF, "", this));
            return scanResult;
}

<SINGLE_QUOTED_STRING> {quote} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<SINGLE_QUOTED_STRING> {quote} :\"" + txt + "\"");
            }
            popState();
            return new ScanResultStringLiteral(ScanResultType.LITERAL, text.toString(), "'", this);

}

<SINGLE_QUOTED_STRING> {xdquote} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<SINGLE_QUOTED_STRING> {xdquote} :\"" + txt + "\"");
            }
            text.append(txt);
}

<SINGLE_QUOTED_STRING> {quoted} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<SINGLE_QUOTED_STRING> {quoted} :\"" + txt + "\"");
            }
            text.append(txt);
}

<SINGLE_QUOTED_STRING> <<EOF>> {
            log.trace("<SINGLE_QUOTED_STRING> <<EOF>>");
            //return new ScanResultImpl(new Msg(MsgKey.valueOf("msg.unterminated.quoted.string"), text.toString()), this);
            log.error("unterminated quoted string");
            ScanResultImpl scanResult = new ScanResultStringLiteral(ScanResultType.LITERAL, text.toString(), "'", this);
            scanResult.setNext(new ScanResultImpl(ScanResultType.EOF, "", this));
            return scanResult;
}

<YYINITIAL> {doublequote} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<YYINITIAL> {doublequote} :\"" + txt + "\"");
            }
            text.setLength(0);
            pushState(DOUBLE_QUOTED_IDENTIFIER);
}

<DOUBLE_QUOTED_IDENTIFIER> {doublequote} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<DOUBLE_QUOTED_IDENTIFIER> {doublequote} :\"" + txt + "\"");
            }
            popState();
            return new ScanResultImpl(ScanResultType.DOUBLE_QUOTED_IDENTIFIER, text.toString(), this);
}

<DOUBLE_QUOTED_IDENTIFIER> {doublequoted} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<DOUBLE_QUOTED_IDENTIFIER> {doublequote} :\"" + txt + "\"");
            }
            text.append(txt);
}

<DOUBLE_QUOTED_IDENTIFIER> <<EOF>> {
            log.trace("<DOUBLE_QUOTED_IDENTIFIER> <<EOF>>");
//            return new ScanResultImpl(new Msg(MsgKey.valueOf("msg.unterminated.quoted.identifier"), text.toString()), this);
            ScanResultImpl scanResult = new ScanResultStringLiteral(ScanResultType.LITERAL, text.toString(), "\"", this);
            scanResult.setNext(new ScanResultImpl(ScanResultType.EOF, "", this));
            return scanResult;

}

<DOUBLE_QUOTED_IDENTIFIER> {newline} {
            log.trace("<DOUBLE_QUOTED_IDENTIFIER> {newline}");
            return new ScanResultImpl(new Msg(MsgKey.valueOf("msg.unterminated.quoted.identifier"), text.toString()), this);
}

<YYINITIAL> {identifier} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<YYINITIAL> {identifier} :\"" + txt + "\"");
            }

            switch (txt.toLowerCase()) {
            // There are two types of "if (not) exists": if exists statement and if exists in a command.
            // Example:
            // 1. IF NOT EXISTS( SELECT 1 FROM tab1) THEN
            //        return a;
            //    END IF;
            // 2. DROP TABLE IF EXISTS tab1;
            // In the first example, the beginEndNestingLevel should be increased while
            // in the second example the beginEndNestingLevel should not be changed.
            case "if":
                if (!endActive) {
                    beginEndNestingLevel++;
                }
                endActive = false;

                ScanResult current = new ScanResultImpl(ScanResultType.IDENTIFIER, txt, this);
                ScanResult first = current.getNextInterpretable();
                if ("not".equalsIgnoreCase(first.getText())) {
                    ScanResult second = first.getNextInterpretable();
                    if ("exists".equalsIgnoreCase(second.getText())) {
                        if (second.getNextInterpretable() != null
                                && !"(".equalsIgnoreCase(second.getNextInterpretable().getText())) {
                            beginEndNestingLevel--;
                            ScanResult end = second.getNextInterpretable();
                            for (ScanResult node = current; node != null; node = node.getNext()) {
                                node.setBeginEndLevel(beginEndNestingLevel);
                                if (node == end) {
                                   if (log.isTraceEnabled()) {
                                     log.trace("beginEndNestingLevel :\"" + beginEndNestingLevel + "\"");
                                   }
                                   return current;
                                }
                            }
                        }
                    }
                } else {
                    if ("exists".equalsIgnoreCase(first.getText())) {
                        if (first.getNextInterpretable() != null
                                && !"(".equalsIgnoreCase(first.getNextInterpretable().getText())) {
                            beginEndNestingLevel--;
                            ScanResult end = first.getNextInterpretable();
                            for (ScanResult node = current; node != null; node = node.getNext()) {
                                node.setBeginEndLevel(beginEndNestingLevel);
                                if (node == end) {
                                   if (log.isTraceEnabled()) {
                                     log.trace("beginEndNestingLevel :\"" + beginEndNestingLevel + "\"");
                                   }
                                   return current;
                                }
                            }
                        }
                    }
                }

                if (log.isTraceEnabled()) {
                  log.trace("beginEndNestingLevel :\"" + beginEndNestingLevel + "\"");
                }
                return current;
            case "not":
                endActive = false;
                break;
            case "begin":
                beginEndNestingLevel++;
                endActive = false;
                break;
            case "case":
            case "loop":
                if (!endActive) {
                    beginEndNestingLevel++;
                }
                endActive = false;
                break;
            case "end":
                beginEndNestingLevel--;
                endActive = true;
                break;
            default:
                endActive = false;
                break;
            }
            if (log.isTraceEnabled()) {
              log.trace("beginEndNestingLevel :\"" + beginEndNestingLevel + "\"");
            }
            return new ScanResultImpl(ScanResultType.IDENTIFIER, txt, this);
}

<YYINITIAL> {word} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<YYINITIAL> {word} :\"" + txt + "\"");
            }
            return new ScanResultImpl(ScanResultType.WORD, txt, this);
}

<YYINITIAL> {semicolon} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<YYINITIAL> {semicolon} :\"" + txt + "\"");
            }
            endActive = false;
            if (parenthesisNestingLevel > 0) {
                parenthesisNestingLevel = 0;
                log.error("msg.too.little.closing.parenthesis");
                ScanResultImpl scanResult = new ScanResultImpl(ScanResultType.SEMI_COLON, txt.toString(), this);
                scanResult.setNext(new ScanResultImpl(ScanResultType.EOF, "", this));
                return scanResult;
            }
            return new ScanResultImpl(ScanResultType.SEMI_COLON, txt, this);
}

<YYINITIAL> {openParenthesis} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<YYINITIAL> {openParenthesis} :\"" + txt + "\"");
            }
            parenthesisNestingLevel++;
            return new ScanResultImpl(ScanResultType.OPENING_PARENTHESIS, txt, this);
}

<YYINITIAL> {closeParenthesis} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<YYINITIAL> {closeParenthesis} :\"" + txt + "\"");
            }
            if (--parenthesisNestingLevel < 0) {
                parenthesisNestingLevel = 0;
            }
            return new ScanResultImpl(ScanResultType.CLOSING_PARENTHESIS, txt, this);
}

<YYINITIAL> {other}|{slash}|{star} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<YYINITIAL> {other}|{slash}|{star} :\"" + txt + "\"");
            }
            endActive = false;
            return new ScanResultImpl(ScanResultType.CHARACTER, txt, this);
}

<YYINITIAL> {dolqdelim}	{
				dollarDollarQuote = yytext();
                if (log.isTraceEnabled()) {
                  log.trace("{dolqdelim} :\"" + dollarDollarQuote + "\"");
                }
		        pushState(xdolq);
                text.setLength(0);
}

<xdolq> {dolqdelim} {

            String txt = yytext();
            if (log.isTraceEnabled()) {
               log.trace("<xdolq> {dolqdelim} :\"" + txt + "\"");
            }
            if (txt.equals(dollarDollarQuote)) {
              popState();
              return new ScanResultStringLiteral(ScanResultType.LITERAL, text.toString(), dollarDollarQuote, this);
            } else {
              /*
               * This was not the dollardollar quote that ends this literal. So push the last dollar back into
               * the stream
               */
              yypushback(1);
              text.append(txt.substring(0, txt.length() - 1));
            }
}

<xdolq> {dolqinside} {
            String txt = yytext();
            if (log.isTraceEnabled()) {
                log.trace("<xdolq> {dolqinside} :\"" + txt + "\"");
            }
            text.append(txt);
}

<xdolq> {other}	{
                  String txt = yytext();
                  if (log.isTraceEnabled()) {
                    log.trace("<other> {word} :\"" + txt + "\"");
                  }
				  /* This is needed for $ inside the quoted text */
				  text.append(txt);
}

<xdolq> <<EOF>> {
            log.trace("<xdolq> <<EOF>>");
            log.error("unterminated dollar dollar quoted string");
            ScanResultImpl scanResult = new ScanResultStringLiteral(ScanResultType.LITERAL, text.toString(), dollarDollarQuote, this);
            scanResult.setNext(new ScanResultImpl(ScanResultType.EOF, "", this));
            return scanResult;
}



<<EOF>> {
            log.trace("<<EOF>>");
            if (parenthesisNestingLevel > 0) {
                parenthesisNestingLevel = 0;
                // return new ScanResultImpl(new Msg(MsgKey.valueOf("msg.too.little.closing.parenthesis"), text.toString()), this);
                log.error("too little closing parenthesis");

                return new ScanResultImpl(ScanResultType.EOF, "", this);
            }
            if (beginEndNestingLevel != 0) {
                Integer value = Integer.valueOf(beginEndNestingLevel);
                beginEndNestingLevel = 0;
                // return new ScanResultImpl(new Msg(MsgKey.valueOf("msg.unbalanced.begin.end"), value), this);
                log.error("unbalanced begin end");
                return new ScanResultImpl(ScanResultType.EOF, "", this);
            }
            return new ScanResultImpl(ScanResultType.EOF, "", this);
}