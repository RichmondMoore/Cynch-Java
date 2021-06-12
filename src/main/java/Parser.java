package main.java;

import java.util.List;

import static main.java.TokenType.*;

class Parser {

    // Parses a single expression and returns it
    // Throws a ParseError if there is a syntax error
    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    // A ParseError is thrown when the parser will be synchronized
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Expr expression() {
        return equality();
    }

    // The following methods are ordered in ascending order of precedence

    // equality --> comparison ( ( "!=" | "==" ) comparison )* ;
    // Matches an equality operator or anything of higher precedence
    // Lowest precedence
    private Expr equality() {
        Expr expr = comparison();

        // Exits the loop if the specified operators are not found.
        // Loop will never be entered if operators are not found.
        // In that scenario, this method would only return a comparison() call
        while(match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        // Going through the entire loop returns a left-associative nested tree
        // of binary operator nodes
        // Ex. a == b == c == d
        // The previous binary expression becomes the left operand of the next

        return expr;
    }

    // comparison --> term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    // Works in the same way as equality()
    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // term --> factor ( ( "-" | "+" ) factor )* ;
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    //factor --> unary ( ( "/" | "*" ) unary )* ;
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // unary --> ( "!" | "-" ) unary | primary ;
    // This method is slightly different, as a unary has only one operand
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    // primary --> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
    // Most cases here are single terminals
    // Highest precedence
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        // Takes the value of the number or string
        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        // Everything beteween the parentheses needs to be grouped
        if (match(LEFT_PAREN)) {
            // Parses the expression and then looks for the closing parenthesis
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression");
            return new Expr.Grouping(expr);
        }

        // If the token doesn't match any of these types, it can't start an expression
        throw error(peek(), "Expect expression.");
    }

    // Checks the current token to see if it has any of the given types
    // If true, the token is consumed
    // Otherwise, the function returns false and does not consume the token
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    // Looks for the closing parenthesis
    // If the group is not closed, throw an error
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    // Returns true if the current token is of the given type
    // Does NOT consume the token
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    // Consumes the current token and returns it
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    // Returns the error rather than throwing it
    // This lets the calling method decide whether or not to unwind the parser
    private ParseError error(Token token, String message) {
        Cynch.error(token, message);
        return new ParseError();
    }

    // After a ParseError is thrown, the exception is caught on statement boundaries
    // After caught, the tokens need to be synchronized
    // This will discard tokens that are likely to cause cascading errors
    private void synchronize() {
        // Consume token
        advance();

        // Consume and discard tokens until the next statment
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;

                default:
                    break;
            }

            advance();
        }
    }
}