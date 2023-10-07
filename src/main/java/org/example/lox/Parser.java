package org.example.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.example.lox.TokenType.AND;
import static org.example.lox.TokenType.BANG;
import static org.example.lox.TokenType.BANG_EQUAL;
import static org.example.lox.TokenType.COMMA;
import static org.example.lox.TokenType.ELSE;
import static org.example.lox.TokenType.EOF;
import static org.example.lox.TokenType.EQUAL;
import static org.example.lox.TokenType.EQUAL_EQUAL;
import static org.example.lox.TokenType.FALSE;
import static org.example.lox.TokenType.FOR;
import static org.example.lox.TokenType.FUN;
import static org.example.lox.TokenType.GREATER;
import static org.example.lox.TokenType.GREATER_EQUAL;
import static org.example.lox.TokenType.IDENTIFIER;
import static org.example.lox.TokenType.IF;
import static org.example.lox.TokenType.LEFT_BRACE;
import static org.example.lox.TokenType.LEFT_PAREN;
import static org.example.lox.TokenType.LESS;
import static org.example.lox.TokenType.LESS_EQUAL;
import static org.example.lox.TokenType.MINUS;
import static org.example.lox.TokenType.NIL;
import static org.example.lox.TokenType.NUMBER;
import static org.example.lox.TokenType.OR;
import static org.example.lox.TokenType.PLUS;
import static org.example.lox.TokenType.PRINT;
import static org.example.lox.TokenType.RIGHT_BRACE;
import static org.example.lox.TokenType.RIGHT_PAREN;
import static org.example.lox.TokenType.SEMICOLON;
import static org.example.lox.TokenType.SLASH;
import static org.example.lox.TokenType.STAR;
import static org.example.lox.TokenType.STRING;
import static org.example.lox.TokenType.TRUE;
import static org.example.lox.TokenType.VAR;
import static org.example.lox.TokenType.WHILE;

/*
Grammar:
============
program        → statement* EOF ;
declaration    → funDecl
               | varDecl
               | statement ;
funDecl        → "fun" function;
function       → IDENTIFIER "(" parameters? ")" block;
varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
statement      → exprStmt
               | ifStmt
               | whileStmt
               | printStmt
               | block;
whileStmt      → "while" "(" expression ")" statement;
ifStmt         → "if" "(" expression ")" statement;
                ("else" statement)?;
block          →  "{" declaration* "}";
exprStmt       → expression ";" ;
printStmt      → "print" expression ";" ;
comma          → expression ("," expression)+;
expression     → assignment;
assignment     → IDENTIFIER "=" assignment
                    | equality
                    | ternaryExpression
                    | logicalOr;
logicalOr      → logicalAnd ("or" logicalAnd)*;
logicalAnd     → equality ("and" equality)*;
ternaryExpression → comparison "?" expression ":" expression;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary
                | primary ;
call           → primary ("(" arguments? ")")*;
arguments      → expression ( "," expression)*;
parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
primary        → "true"
                   | "false"
                   | "nil"
                   | "(" expression ")"
                   | IDENTIFIER;
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

//    private Expr expression()
//    {
//        Expr expr = comparison();
//
//        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
//            Token operator = previous();
//            Expr right = comparison();
//            expr = new Expr.Binary(expr, operator, right);
//        }
//
//        if (match(QUESTION_MARK)) {
//            Token question = previous();
//            Expr left = expression();
//            consume(TokenType.COLON, "Ternary format error");
//            Token colon = previous();
//            Expr right = expression();
//            return new Expr.Ternary(expr, question, left, colon, right);
//        }
//
//        return expr;
//    }

    private Expr expression()
    {
        return assignment();
    }

    List<Stmt> parse()
    {
        try {
            List<Stmt> statements = new ArrayList<>();
            while (!isAtEnd()) {
                statements.add(declaration());
            }

            return statements;
        }
        catch (ParseError parseError) {
            Lox.error(1, parseError.toString());
        }
        return null;
    }

    private Stmt declaration()
    {
        try {
            if (match(FUN)) {
                return function("function");
            }
            if (match(VAR)) {
                return varDeclaration();
            }
            return statement();
        }
        catch (ParseError error) {
//            sychronize();
        }
        return null;
    }

    private Stmt.Function function(String kind)
    {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");

        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }

                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");

        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    private Stmt varDeclaration()
    {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt statement()
    {
        if (match(IF)) {
            return ifStatement();
        }

        if (match(PRINT)) {
            return printStatement();
        }

        if (match(LEFT_BRACE)) {
            return new Stmt.Block(block());
        }

        if (match(WHILE)) {
            return whileStatement();
        }

        if (match(FOR)) {
            return forStatement();
        }

        return expressionStatement();
    }

    private Stmt forStatement()
    {
        consume(LEFT_PAREN, "Expect '(' after 'for'");
        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        }
        else if (match(VAR)) {
            initializer = varDeclaration();
        }
        else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expected ';' after for loop condition");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expected ')' after for loop increment");

        Stmt body = statement();

        if (condition == null) {
            condition = new Expr.Literal(true);
        }

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }
        body = new Stmt.While(condition, body);
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt whileStatement()
    {
        consume(LEFT_PAREN, "Expect '(' after 'while'");
        Expr condition = expression();
        consume(RIGHT_BRACE, "Expect ')' after while condition");

        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    private Stmt ifStatement()
    {
        consume(LEFT_PAREN, "Expect '(' after 'if'");
        Expr condition = expression();
        consume(RIGHT_BRACE, "Expect ')' after if condition");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private List<Stmt> block()
    {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Stmt printStatement()
    {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt expressionStatement()
    {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Expr assignment()
    {
        Expr expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }

            throw error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or()
    {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr and()
    {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
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

        return call();
    }

    private Expr call()
    {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            }
            else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee)
    {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    throw error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            }
            while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

//    private List<Expr> argument()
//    {
//        List<Expr> result = new ArrayList<>();
//        result.add(expression());
//        while (match(COMMA)) {
//            result.add(expression());
//        }
//        return result;
//    }

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

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
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
