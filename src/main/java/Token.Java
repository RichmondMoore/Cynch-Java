package main.java;

class Token {
    final TokenType type;   // The type of Token (ex. *, +, number, class, etc.)
    final String lexeme;    // The raw substring of code
    final Object literal;   // The actual value used at runtime
    final int line;         // The line the Token is located

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    // Returns information about the Token
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}