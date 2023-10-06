package org.example.lox;

public class RpnPrinter
        implements Expr.Visitor<String>
{
    @Override
    public String visitAssignExpr(Expr.Assign expr)
    {
        return null;
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr)
    {
        return visit(expr.left) + " " + visit(expr.right) + " " + expr.operator.lexeme;
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr)
    {
        return null;
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr)
    {
        if (expr.value == null) {
            return "nil";
        }
        return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr)
    {
        return null;
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr)
    {
        return null;
    }

    @Override
    public String visitTernaryExpr(Expr.Ternary expr)
    {
        return null;
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr)
    {
        return null;
    }

    public static void main(String[] args)
    {
        Expr expr = new Expr.Binary(
                new Expr.Binary(
                        new Expr.Literal(1),
                        new Token(TokenType.PLUS, "+", null, 1),
                        new Expr.Literal(2)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Binary(
                        new Expr.Literal(4),
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(3)
                ));

        System.out.println(new RpnPrinter().visit(expr));
    }
}
