package org.example.lox;

import java.util.List;

import static org.example.lox.TokenType.BANG;
import static org.example.lox.TokenType.BANG_EQUAL;
import static org.example.lox.TokenType.COMMA;
import static org.example.lox.TokenType.EOF;
import static org.example.lox.TokenType.EQUAL_EQUAL;
import static org.example.lox.TokenType.FALSE;
import static org.example.lox.TokenType.GREATER;
import static org.example.lox.TokenType.GREATER_EQUAL;
import static org.example.lox.TokenType.LEFT_PAREN;
import static org.example.lox.TokenType.LESS;
import static org.example.lox.TokenType.LESS_EQUAL;
import static org.example.lox.TokenType.MINUS;
import static org.example.lox.TokenType.NIL;
import static org.example.lox.TokenType.NUMBER;
import static org.example.lox.TokenType.PLUS;
import static org.example.lox.TokenType.QUESTION_MARK;
import static org.example.lox.TokenType.RIGHT_PAREN;
import static org.example.lox.TokenType.SLASH;
import static org.example.lox.TokenType.STAR;
import static org.example.lox.TokenType.STRING;
import static org.example.lox.TokenType.TRUE;

/*
Grammar:
============
Added: (comma, ternary)
comma          → expression ("," expression)+;
============
expression     → equality | ternaryExpression;
ternaryExpression → comparison "?" expression ":" expression;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary
               | primary ;
primary        → NUMBER | STRING | "true" | "false" | "nil"
               | "(" expression ")" ;
*/

public class Parser
{
    private static class ParseError
            extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens)
    {
        this.tokens = tokens;
    }

    private Expr comma()
    {
        Expr expr = expression();

        while (match(COMMA)) {
            Expr right = expression();
            expr = right;
        }
        return expr;
    }

    private Expr expression()
    {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        if (match(QUESTION_MARK)) {
            Token question = previous();
            Expr left = expression();
            consume(TokenType.COLON, "Ternary format error");
            Token colon = previous();
            Expr right = expression();
            return new Expr.Ternary(expr, question, left, colon, right);
        }

        return expr;
    }

    Expr parse() {
        try {
            return comma();
        } catch (ParseError error) {
            return null;
        }
    }

    private Expr equality()
    {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison()
    {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term()
    {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor()
    {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary()
    {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary()
    {
        if (match(FALSE)) {
            return new Expr.Literal(false);
        }
        if (match(TRUE)) {
            return new Expr.Literal(true);
        }
        if (match(NIL)) {
            return new Expr.Literal(null);
        }

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private boolean match(TokenType... types)
    {
        for (TokenType tokenType : types) {
            if (check(tokenType)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message)
    {
        if (check(type)) {
            return advance();
        }

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message)
    {
        Lox.error(token, message);
        return new ParseError();
    }

    private boolean check(TokenType type)
    {
        if (isAtEnd()) {
            return false;
        }
        return peek().type == type;
    }

    private Token advance()
    {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd()
    {
        return peek().type == EOF;
    }

    private Token peek()
    {
        return tokens.get(current);
    }

    private Token previous()
    {
        return tokens.get(current - 1);
    }
}
