package main.java;

import java.util.List;

// Each expression type extends Expr so that each one has a Visitor
// This also aids in debugging as an error will be thrown if trying to access
// a field that the class does not have (ex. trying to do something with 
// Grouping.left)

// 
abstract class Expr {
    interface Visitor<R> {
        R visitBinaryExpr(Binary expr);
        R visitGroupingExpr(Grouping expr);
        R visitLiteralExpr(Literal expr);
        R visitUnaryExpr(Unary expr);
    }

  // Operators that need two operands (ex. +, -, *, /, and logic operators)
  static class Binary extends Expr {
    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

  @Override
  <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
  }

    final Expr left;
    final Token operator;
    final Expr right;
  }

  // A pair of parentheses grouping an expression
  static class Grouping extends Expr {
    Grouping(Expr expression) {
      this.expression = expression;
    }

  @Override
  <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpr(this);
  }

    final Expr expression;
  }

  // No operands (ex. numbers, strings, true, false)
  static class Literal extends Expr {
    Literal(Object value) {
      this.value = value;
    }

  @Override
  <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
  }

    final Object value;
  }

  // One operand (ex. - or !)
  static class Unary extends Expr {
    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

  @Override
  <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
  }

    final Token operator;
    final Expr right;
  }

  abstract <R> R accept(Visitor<R> visitor);
}

/*
    Cynch Grammer:

    expression     → literal
                   | unary
                   | binary
                   | grouping ;

    literal        → NUMBER | STRING | "true" | "false" | "nil" ;
    grouping       → "(" expression ")" ;
    unary          → ( "-" | "!" ) expression ;
    binary         → expression operator expression ;
    operator       → "==" | "!=" | "<" | "<=" | ">" | ">="
                   | "+"  | "-"  | "*" | "/" ;
*/