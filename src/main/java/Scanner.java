package main.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static main.java.TokenType.*;

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line);
        return tokens;
    }

    private void scanToken() {
        char c = advance();

        switch(c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/':
                if (match('/')) {
                    // Ignores comments (goes to end of line)
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace
                break;
            case '\n':
                line++;
                break;
            case '"': string(); break; // Strings always begin with "

            // TODO combine multiple invalid characters errors into one error
            default:
                Cynch.error(line, "Unexpected character.");
                break;
        }
    }

    // ***** Helper Functions ***** //

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    @Override
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    // Consumes the current character ONLY if it is expected
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    // Works like advance, but does not consume characters
    // One character of lookahead
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    // Detects the entire string and checks if it is terminated
    // Adds a token with the value of the string
    private void string() {
        // Consume characters until the string is terminated
        while (peek() != '"' && !isAtEnd()) {
            // Multi-line strings are supported, so 'line' needs to be incremented
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Cynch.error(line, "Unterminated string.");
            return;
        }

        // The terminating "
        advance();

        // Trim quotes
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }
}
