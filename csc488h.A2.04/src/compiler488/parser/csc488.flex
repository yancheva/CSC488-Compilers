/* $Id: csc488.flex,v 1.1 2015/01/15 16:02:00 dw Exp dw $ */

/* This is one of the original comments, included for attribution:
 *
 * File Name: csc488.flex
 * To Create: jflex csc488.flex
 *
 * and then after the parser is created
 * > javac Lexer.java
 */

/* This is the user-code section.  
 * It will be copied verbatim into the class generated by JFlex.
 */
package compiler488.parser;

import java_cup.runtime.*;
import compiler488.parser.sym;		// cup generated symbols
      
%%
/* This is the options-and-declarations section. */

/* This option specifies both the name of the class generated by JFlex, and
 * the name of the file to put it in.
 */
%class Lexer
%public

/* This option specifies the character set the scanner will work on. */
%unicode

/* This option sets up the interface to work with a parser generated by CUP. */
%cup

/* These options turn on line and column counting. */
%line
%column

/* The code between %{ and }% will be copied verbatim into the source file
 * for the lexer class.  %{ and }% must both be at the beginning of a line.
 */
%{
    /* The interface to the CUP generated parser requires that the lexer
     * return java_cup.runtime.Symbol objects.  Each Symbol must have a
     * `type`, which is an integer value, and must have a corresponding
     * entry in sym.java.  The file sym.java is generated by CUP.  The
     * entries in sym.java correspond to the terminals defined in the
     * CUP parser specification.
     *
     * Here are some functions to help create Symbols for CUP.
     */

    /* For tokens with no value. */
    private Symbol symbol(int type)
	{
        return new Symbol(type, yyline, yycolumn);
	}
    
    /* For tokens with a value of type Object. */
    private Symbol symbol(int type, Object value)
	{
        return new Symbol(type, yyline, yycolumn, value);
	}

%}

/* Declare macros here.  Macros are regular expressions that are used
 * by placing the macro name between curly braces (eg, {digit}).
 */
digit	= [0-9]

/* This pattern matches valid identifiers. */
ident 	= [A-Za-z][A-Za-z0-9_]*

/* This pattern matches text constants.  It does not support strings
 * that contain the " character or any other escapes.
 * Text constants are terminated by a newline character to catch
 * unterminated string errors
 */
text_lit= \"[^\"\n\r]*[\"\n\r]

/* This pattern matches   % rest of line   style comments.
   Note that it handles line ending conventions for Linux, Windows and MAC
 */

/* we may not need this anymore. Line terminators are ignored
 * anyway by ignoring white spaces
 */
/* LineTerminator = \r|\n|\r\n */
InputCharacter = [^\r\n]
EndOfLineComment     = "%" {InputCharacter}*

%%
/* This is the lexical-rules section. */

/* This section contains regular expressions and actions, i.e. Java
 * code, that will be executed when the scanner matches the associated
 * regular expression.
 *
 * YYINITIAL is the state at which the lexer begins scanning.  So
 * these regular expressions will only be matched if the scanner is in
 * the start state YYINITIAL.
 */

<YYINITIAL> {
    "("			{ return symbol(sym.L_PAREN);	}
    ")"			{ return symbol(sym.R_PAREN);	}
    "["			{ return symbol(sym.L_SQUARE);	}
    "]"			{ return symbol(sym.R_SQUARE);	}
    "{"			{ return symbol(sym.L_CURLEY);	}
    "}"			{ return symbol(sym.R_CURLEY);	}
    ">"			{ return symbol(sym.GREATER);	}
    "<"			{ return symbol(sym.LESS);	}
    "+"			{ return symbol(sym.PLUS);	}
    "-"			{ return symbol(sym.MINUS);	}
    "*"			{ return symbol(sym.TIMES);	}
    "/"			{ return symbol(sym.DIVIDE);	}
    "="                	{ return symbol(sym.EQUAL);	}
    "."			{ return symbol(sym.DOT);	}
    ","			{ return symbol(sym.COMMA);	}
    "!"	   		{ return symbol(sym.NOT);	}
    "&"			{ return symbol(sym.AND);	}
    "|"			{ return symbol(sym.OR);	}

    "true"		{ return symbol(sym.TRUE);	}
    "false"		{ return symbol(sym.FALSE);	}
    "integer"		{ return symbol(sym.INTEGER);	}
    "boolean"		{ return symbol(sym.BOOLEAN);	}
    "function"		{ return symbol(sym.FUNCTION);	}
    "procedure"		{ return symbol(sym.PROCEDURE);	}
    
    "begin"		{ return symbol(sym.BEGIN);	}
    "do"		{ return symbol(sym.DO);	}
    "else"		{ return symbol(sym.ELSE);	}
    "end"		{ return symbol(sym.END);	}
    "exit"		{ return symbol(sym.EXIT);	}
    "get"		{ return symbol(sym.GET);	}
    "if"		{ return symbol(sym.IF);	}
    "loop"		{ return symbol(sym.LOOP);	}
    "put"		{ return symbol(sym.PUT);	}
    "return"		{ return symbol(sym.RETURN);	}
    "skip"		{ return symbol(sym.SKIP);	}
    "then"		{ return symbol(sym.THEN);	}
    "when"		{ return symbol(sym.WHEN);	}
    "while"		{ return symbol(sym.WHILE);	}
    "yields"		{ return symbol(sym.YIELDS);	}

    {digit}+		{ return symbol(sym.INTCONST,
						Integer.valueOf (yytext())); }
    {ident}		{ return symbol(sym.IDENT, yytext ()); }

    /* When a text constant is found, return the string without the quotes. */
    {text_lit}		{ return symbol(sym.TEXTCONST,
				yytext().substring(1,yytext().length()-1)); }

    [ \t\f\r\n]		{ /* ignore whitespace */ }   

    {EndOfLineComment}	{ /* discard comments  */ }

}

/* No token was found for the input so throw an error.  Print out an
 * Illegal character message with the illegal character that was found.
 */
\\[^]                    { throw new RuntimeException("Illegal character <"+yytext()+">"); }
[^]                    { String st = "Error" ;
                         if( yyline >= 0 )
			 {
                            st += " in line " + ( yyline + 1 ) ;
                            if( yycolumn >= 0 )
                              st += ", column " + (yycolumn + 1 ) ;
                         }
			 else
                            st += " at end of input " ;
                         st += ": Illegal character <" + yytext() + ">";
                         System.err.println( st );
		       }


