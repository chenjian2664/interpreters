package org.example.lox;

public class Interpreter
        implements Expr.Visitor<Object>
{
    @Override
    public Object visitBinaryExpr(Expr.Binary expr)
    {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG_EQUAL -> {
                return !isEqual(left, right);
            }
            case EQUAL_EQUAL -> {
                return isEqual(left, right);
            }
            case LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            }
            case LESS -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            }
            case GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            }
            case GREATER -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            }
            case MINUS -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            }
            case PLUS -> {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String || right instanceof String) {
                    return left.toString() + right.toString();
                }
            }
            case SLASH -> {
                checkNumberOperands(expr.operator, left, right);
                if ((double)right == 0) {
                    throw new RuntimeError(expr.operator, "Divided by the /0");
                }
                return (double) left / (double) right;
            }
            case STAR -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            }
        }

        return null;
    }

    private void checkNumberOperand(Token operator, Object operand)
    {
        if (operand instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right)
    {
        if (left instanceof Double && right instanceof Double) {
            return;
        }

        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isEqual(Object a, Object b)
    {
        if (a == null && b == null) {
            return true;
        }
        if (a == null) {
            return false;
        }

        return a.equals(b);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr)
    {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr)
    {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr)
    {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -((double) right);
            case BANG:
//                return !((boolean) right);
                return !isTruthy(right);
        }

        return null;
    }

    private boolean isTruthy(Object object)
    {
        if (object == null) {
            return false;
        }
        if (object instanceof Boolean) {
            return (boolean) object;
        }
        return true;
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr)
    {
        boolean evaluate = (boolean) evaluate(expr.compare);
        return evaluate ? evaluate(expr.left) : evaluate(expr.right);
    }

    private Object evaluate(Expr expr)
    {
        return expr.accept(this);
    }

    void interpret(Expr expression)
    {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        }
        catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private String stringify(Object object)
    {
        if (object == null) {
            return "nil";
        }

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    static class RuntimeError
            extends RuntimeException
    {
        final Token token;

        RuntimeError(Token token, String message)
        {
            super(message);
            this.token = token;
        }
    }
}
