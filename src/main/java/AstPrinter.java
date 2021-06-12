package main.java;

class AstPrinter implements Expr.Visitor<String> {
    
    // This takes the given expression and passes it to the correct method
    String print(Expr expr) {
        return expr.accept(this);
    }

    // These visit methods determine how each expression is converted to a String
    // The 'parenthesize' calls become nested, as the left side of a Binary expression could be
    // a Grouping or Unary expression, which both must call 'parenthesize'
    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {

        // Converts Java's 'null' to Cynch's 'nil'
        if (expr.value == null) return "nil";

        // This does not have multiple expressions, so only the value is needed
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    // Takes the name of the lexeme (ex. MINUS) and a variable amount of expressions (to handle any length expression)
    // Produces a String to represent that portion of the tree (ex. (- 123))
    private String parenthesize(String name, Expr...exprs) {
        StringBuilder builder = new StringBuilder();

        // For each expression, 
        builder.append("(").append(name);

        // For each sub-expression in the expression (as each subclass has a different number)
        for (Expr expr : exprs) {
            builder.append(" ");

            // Each expression is handled differently when converted to a String
            // Ex. a Grouping has "group" in front of the value
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }

    // Prints the AST (Abstract Syntax Tree) for the given expression
    // Note that this method is only for visualization purposes
    public static void main(String[] args) {

        // -123 * (45.67) --> (* (- 123) (group 45.67))
        Expr expression = new Expr.Binary(
            new Expr.Unary(
                new Token(TokenType.MINUS, "-", null, 1),   // Operator (UNARY)
                new Expr.Literal(123)),                     // Right side of operator (UNARY)
            new Token(TokenType.STAR, "*", null, 1),        // Operator (BINARY)
            new Expr.Grouping(
                new Expr.Literal(45.67)));                  // Expression (GROUPING)

        System.out.println(new AstPrinter().print(expression));
        System.out.println();

        // (1 >= 2) == (1 + (0 * 2)) --> (* (group (>= 1 2)) (group (+ 1 (group (* 0 2)))))
        Expr expression2 = new Expr.Binary(
            new Expr.Grouping(
                new Expr.Binary(
                    new Expr.Literal(1), 
                    new Token(TokenType.GREATER_EQUAL, ">=", null, 1), 
                    new Expr.Literal(2)
                )
            ), 
            new Token(TokenType.STAR, "*", null, 1), 
            new Expr.Grouping(
                new Expr.Binary(
                    new Expr.Literal(1), 
                    new Token(TokenType.PLUS, "+", null, 1), 
                    new Expr.Grouping(
                        new Expr.Binary(
                            new Expr.Literal(0), 
                            new Token(TokenType.STAR, "*", null, 1), 
                            new Expr.Literal(2)
                        )
                    )
                )
            )
        );

        System.out.println(new AstPrinter().print(expression2));
    }
}
