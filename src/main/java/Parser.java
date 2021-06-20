package main.java;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import static main.java.TokenType.*;

class Parser {

    // Parses a single expression and returns it
    // Throws a ParseError if there is a syntax error
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    // A ParseError is thrown when the parser will be synchronized
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;
    private int loopDepth = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Expr expression() {
        return assignment();
    }

    // Entry point for parse()
    // Checks for variable delcaration, otherwise looks for another statement
    // Synchronizes the parser if there is an error
    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(BREAK)) return breakStatement();
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt breakStatement() {
        if (loopDepth == 0) {
            error(previous(), "Must be inside loop to use 'break'.");
        }
        consume(SEMICOLON, "Expect ';' after 'break'.");
        return new Stmt.Break();
    }

    // This can be built using already defined control statements
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null; // A new variable does not have to be declared
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            // If no variable is declared, there must be an expression here
            initializer = expressionStatement(); 
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        try {
            loopDepth++;
            Stmt body = statement();

            if (increment != null) {
                body = new Stmt.Block(
                    Arrays.asList(body, new Stmt.Expression(increment))
                );
            }

            if (condition == null) condition = new Expr.Literal(true);
            body = new Stmt.While(condition, body);

            if (initializer != null) {
                body = new Stmt.Block(Arrays.asList(initializer, body));
            }

            return body;
        } finally {
            loopDepth--;
        }

        
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        // Since a variable can be declared without being initialized,
        // pass a null if there is no equals sign
        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        // Check for ending semicolon and return the variable statement
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after while condition.");

        try {
            loopDepth++;

            Stmt body = statement();

            return new Stmt.While(condition, body);
        } finally {
            loopDepth--;
        }
}

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr assignment() {
        Expr expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
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

    // TODO add support for bitwise below equality()

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

        // Looks for an identifier to check for a variable
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
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
