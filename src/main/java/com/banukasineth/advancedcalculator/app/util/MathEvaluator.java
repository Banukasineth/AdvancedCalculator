package com.banukasineth.advancedcalculator.app.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Robust, offline mathematical expression evaluator supporting:
 * - Basic arithmetic (+, -, *, /)
 * - Operator precedence (* and / over + and -)
 * - Powers (^, ^2, ^{-1})
 * - Square root (\sqrt)
 * - Percentages (%, \%) including standalone and relative (A + B%)
 * - Parentheses and LaTeX braces
 * - Unary minus and negation
 * - Factorials (!)
 * - Standard trigonometric and logarithmic functions
 * - Clean number formatting (integers without decimal points, avoiding IEEE 754 precision artifacts)
 */
public class MathEvaluator {

    public static class EvaluationResult {
        private final boolean success;
        private final double value;
        private final String formattedResult;
        private final String errorMessage;

        private EvaluationResult(double value, String formattedResult) {
            this.success = true;
            this.value = value;
            this.formattedResult = formattedResult;
            this.errorMessage = null;
        }

        private EvaluationResult(String errorMessage) {
            this.success = false;
            this.value = Double.NaN;
            this.formattedResult = errorMessage;
            this.errorMessage = errorMessage;
        }

        public static EvaluationResult ok(double value) {
            return new EvaluationResult(value, formatNumber(value));
        }

        public static EvaluationResult error(String message) {
            return new EvaluationResult(message);
        }

        public boolean isSuccess() {
            return success;
        }

        public double getValue() {
            return value;
        }

        public String getFormattedResult() {
            return formattedResult;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static EvaluationResult evaluate(String rawExpression) {
        if (rawExpression == null || rawExpression.trim().isEmpty()) {
            return EvaluationResult.error("Empty expression");
        }

        try {
            String normalized = normalizeLatex(rawExpression);
            List<Token> tokens = tokenize(normalized);
            if (tokens.isEmpty() || (tokens.size() == 1 && tokens.get(0).type == TokenType.EOF)) {
                return EvaluationResult.error("Empty expression");
            }

            Parser parser = new Parser(tokens);
            double result = parser.parse();
            return EvaluationResult.ok(result);
        } catch (ArithmeticException ex) {
            return EvaluationResult.error(ex.getMessage());
        } catch (Exception ex) {
            return EvaluationResult.error("Invalid expression");
        }
    }

    /**
     * Formats a double value cleanly:
     * - Whole numbers without trailing ".0"
     * - Rounding to 10 decimal places to eliminate floating point artifacts (e.g. 0.1 + 0.2 = 0.3)
     * - Scientific notation for extreme numbers
     */
    public static String formatNumber(double val) {
        if (Double.isNaN(val)) return "Error";
        if (Double.isInfinite(val)) return val > 0 ? "Infinity" : "-Infinity";

        // Check for integer within tiny tolerance
        double rounded = Math.round(val);
        if (Math.abs(val - rounded) < 1e-11) {
            return String.valueOf((long) rounded);
        }

        if (Math.abs(val) >= 1e12 || (Math.abs(val) > 0 && Math.abs(val) < 1e-7)) {
            return String.format(Locale.US, "%.6e", val).replace("e+0", "e+").replace("e-0", "e-");
        }

        try {
            BigDecimal bd = BigDecimal.valueOf(val).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros();
            return bd.toPlainString();
        } catch (Exception e) {
            return String.valueOf(val);
        }
    }

    /**
     * Normalizes LaTeX mathematical syntax into a clean infix representation.
     */
    public static String normalizeLatex(String input) {
        if (input == null) return "";
        String s = input.trim();

        // Remove LaTeX formatting environments
        s = s.replace("\\begin{aligned}", "")
             .replace("\\end{aligned}", "")
             .replace("\\mathbf{", "")
             .replace("\\text{", "")
             .replace("\\operatorname{Re}", "Re")
             .replace("\\operatorname{Im}", "Im");

        // Operators
        s = s.replace("\\times", "*")
             .replace("\\cdot", "*")
             .replace("\\div", "/")
             .replace("\\bmod", "%")
             .replace("\\%", "%");

        // Square roots: \sqrt{...} -> sqrt(...)
        s = s.replaceAll("\\\\sqrt\\s*\\{", "sqrt(")
             .replaceAll("\\\\sqrt\\s*\\(", "sqrt(");

        // Exponents: ^{...} -> ^(...)
        s = s.replaceAll("\\^\\s*\\{", "^(")
             .replaceAll("\\^\\s*2", "^(2)");

        // Functions: \sin, \cos, \tan, etc.
        s = s.replaceAll("\\\\([a-zA-Z]+)", "$1");

        // Constants: π, pi -> (3.141592653589793)
        s = s.replace("π", "(3.14159265358979323846)")
             .replaceAll("\\bpi\\b", "(3.14159265358979323846)");

        // Constant e: standalone e
        s = s.replaceAll("(?<=[^a-zA-Z0-9_]|^)e(?=[^a-zA-Z0-9_]|$)", "(2.71828182845904523536)");

        // Convert any remaining { and } to ( and )
        s = s.replace('{', '(').replace('}', ')');

        // Auto-close open parentheses
        int openCount = 0;
        for (char c : s.toCharArray()) {
            if (c == '(') openCount++;
            else if (c == ')') openCount--;
        }
        StringBuilder sb = new StringBuilder(s);
        while (openCount > 0) {
            sb.append(')');
            openCount--;
        }

        return sb.toString();
    }

    // ==========================================
    // Tokenizer & Parser
    // ==========================================

    private enum TokenType {
        NUMBER, PLUS, MINUS, MUL, DIV, MOD, POW, PERCENT,
        LPAREN, RPAREN, FACTORIAL, IDENT, EOF
    }

    private static class Token {
        final TokenType type;
        final String text;
        final double numberValue;

        Token(TokenType type, String text, double numberValue) {
            this.type = type;
            this.text = text;
            this.numberValue = numberValue;
        }

        Token(TokenType type, String text) {
            this(type, text, 0.0);
        }

        @Override
        public String toString() {
            return type + (text != null ? "(" + text + ")" : "");
        }
    }

    private static List<Token> tokenize(String expr) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = expr.length();

        while (i < n) {
            char c = expr.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // Number: integer or decimal
            if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(expr.charAt(i + 1)))) {
                int start = i;
                boolean hasDot = (c == '.');
                i++;
                while (i < n && (Character.isDigit(expr.charAt(i)) || (!hasDot && expr.charAt(i) == '.'))) {
                    if (expr.charAt(i) == '.') hasDot = true;
                    i++;
                }
                // Optional scientific notation: e.g. 1e-5
                if (i < n && (expr.charAt(i) == 'e' || expr.charAt(i) == 'E')
                        && i + 1 < n && (Character.isDigit(expr.charAt(i + 1))
                        || ((expr.charAt(i + 1) == '+' || expr.charAt(i + 1) == '-')
                        && i + 2 < n && Character.isDigit(expr.charAt(i + 2))))) {
                    i++;
                    if (expr.charAt(i) == '+' || expr.charAt(i) == '-') i++;
                    while (i < n && Character.isDigit(expr.charAt(i))) i++;
                }

                String numStr = expr.substring(start, i);
                double val = Double.parseDouble(numStr);

                // Insert implicit multiplication if preceded by a closing paren or percent
                if (!tokens.isEmpty()) {
                    TokenType prev = tokens.get(tokens.size() - 1).type;
                    if (prev == TokenType.RPAREN || prev == TokenType.PERCENT || prev == TokenType.FACTORIAL) {
                        tokens.add(new Token(TokenType.MUL, "*"));
                    }
                }

                tokens.add(new Token(TokenType.NUMBER, numStr, val));
                continue;
            }

            // Word / Identifier (function name: sin, cos, sqrt, log, etc.)
            if (Character.isLetter(c)) {
                int start = i;
                while (i < n && Character.isLetter(expr.charAt(i))) {
                    i++;
                }
                String word = expr.substring(start, i);

                // Insert implicit multiplication if preceded by number, rparen, or percent
                if (!tokens.isEmpty()) {
                    TokenType prev = tokens.get(tokens.size() - 1).type;
                    if (prev == TokenType.NUMBER || prev == TokenType.RPAREN
                            || prev == TokenType.PERCENT || prev == TokenType.FACTORIAL) {
                        tokens.add(new Token(TokenType.MUL, "*"));
                    }
                }

                tokens.add(new Token(TokenType.IDENT, word));
                continue;
            }

            // Operators and punctuation
            switch (c) {
                case '+':
                    tokens.add(new Token(TokenType.PLUS, "+"));
                    i++;
                    break;
                case '-':
                    tokens.add(new Token(TokenType.MINUS, "-"));
                    i++;
                    break;
                case '*':
                    tokens.add(new Token(TokenType.MUL, "*"));
                    i++;
                    break;
                case '/':
                    tokens.add(new Token(TokenType.DIV, "/"));
                    i++;
                    break;
                case '%':
                    tokens.add(new Token(TokenType.PERCENT, "%"));
                    i++;
                    break;
                case '^':
                    tokens.add(new Token(TokenType.POW, "^"));
                    i++;
                    break;
                case '!':
                    tokens.add(new Token(TokenType.FACTORIAL, "!"));
                    i++;
                    break;
                case '(':
                    if (!tokens.isEmpty()) {
                        TokenType prev = tokens.get(tokens.size() - 1).type;
                        if (prev == TokenType.NUMBER || prev == TokenType.RPAREN
                                || prev == TokenType.PERCENT || prev == TokenType.FACTORIAL) {
                            tokens.add(new Token(TokenType.MUL, "*"));
                        }
                    }
                    tokens.add(new Token(TokenType.LPAREN, "("));
                    i++;
                    break;
                case ')':
                    tokens.add(new Token(TokenType.RPAREN, ")"));
                    i++;
                    break;
                default:
                    i++;
                    break;
            }
        }

        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }

    private static class Parser {
        private final List<Token> tokens;
        private int current = 0;

        Parser(List<Token> tokens) {
            this.tokens = tokens;
        }

        double parse() {
            double res = expression();
            if (!isAtEnd()) {
                throw new IllegalArgumentException("Unexpected token after expression: " + peek().text);
            }
            return res;
        }

        // Handles + and -
        private double expression() {
            double result = term();

            // Standalone percent at root expression: e.g. 50% -> 0.5
            if (match(TokenType.PERCENT)) {
                result = result / 100.0;
            }

            while (match(TokenType.PLUS, TokenType.MINUS)) {
                TokenType op = previous().type;
                double right = term();

                // Check for percentage applied to addition/subtraction:
                // e.g. 200 + 10% -> 200 + (200 * 10 / 100) = 220
                // e.g. 200 - 15% -> 200 - (200 * 15 / 100) = 170
                if (match(TokenType.PERCENT)) {
                    right = Math.abs(result) * (right / 100.0);
                }

                if (op == TokenType.PLUS) {
                    result += right;
                } else {
                    result -= right;
                }
            }

            return result;
        }

        // Handles *, /, % (modulo / multiplication / division)
        private double term() {
            double result = factor();

            while (match(TokenType.MUL, TokenType.DIV, TokenType.MOD)) {
                TokenType op = previous().type;
                double right = factor();

                if (match(TokenType.PERCENT)) {
                    right = right / 100.0;
                }

                if (op == TokenType.MUL) {
                    result *= right;
                } else if (op == TokenType.DIV) {
                    if (right == 0.0) {
                        throw new ArithmeticException("Cannot divide by zero");
                    }
                    result /= right;
                } else {
                    if (right == 0.0) {
                        throw new ArithmeticException("Cannot divide by zero");
                    }
                    result %= right;
                }
            }

            return result;
        }

        // Handles powers (^) right-associative, postfix factorial
        private double factor() {
            double result = unary();

            while (match(TokenType.FACTORIAL)) {
                result = factorial(result);
            }

            if (match(TokenType.POW)) {
                double exponent = factor(); // right-associative recursion
                result = Math.pow(result, exponent);
            }

            return result;
        }

        // Handles unary plus and minus
        private double unary() {
            if (match(TokenType.PLUS)) {
                return unary();
            }
            if (match(TokenType.MINUS)) {
                return -unary();
            }
            return primary();
        }

        // Handles numbers, parentheses, functions
        private double primary() {
            if (match(TokenType.NUMBER)) {
                return previous().numberValue;
            }

            if (match(TokenType.LPAREN)) {
                double val = expression();
                consume(TokenType.RPAREN, "Expected ')' after expression");
                return val;
            }

            if (match(TokenType.IDENT)) {
                String func = previous().text.toLowerCase();

                boolean hasParen = match(TokenType.LPAREN);
                double arg = expression();
                if (hasParen) {
                    consume(TokenType.RPAREN, "Expected ')' after function argument");
                }

                return evaluateFunction(func, arg);
            }

            throw new IllegalArgumentException("Unexpected token: " + peek().text);
        }

        private double evaluateFunction(String func, double arg) {
            switch (func) {
                case "sqrt":
                    if (arg < 0) throw new ArithmeticException("Invalid input");
                    return Math.sqrt(arg);
                case "cbrt":
                    return Math.cbrt(arg);
                case "sin":
                    return Math.sin(Math.toRadians(arg));
                case "cos":
                    return Math.cos(Math.toRadians(arg));
                case "tan":
                    // Check for tan(90 + 180k)
                    double mod = Math.abs(arg) % 180;
                    if (Math.abs(mod - 90) < 1e-9) {
                        throw new ArithmeticException("Invalid input");
                    }
                    return Math.tan(Math.toRadians(arg));
                case "asin":
                    if (arg < -1 || arg > 1) throw new ArithmeticException("Invalid input");
                    return Math.toDegrees(Math.asin(arg));
                case "acos":
                    if (arg < -1 || arg > 1) throw new ArithmeticException("Invalid input");
                    return Math.toDegrees(Math.acos(arg));
                case "atan":
                    return Math.toDegrees(Math.atan(arg));
                case "log":
                case "log10":
                    if (arg <= 0) throw new ArithmeticException("Invalid input");
                    return Math.log10(arg);
                case "ln":
                    if (arg <= 0) throw new ArithmeticException("Invalid input");
                    return Math.log(arg);
                case "abs":
                    return Math.abs(arg);
                case "floor":
                    return Math.floor(arg);
                case "ceil":
                    return Math.ceil(arg);
                case "re":
                    return arg;
                case "im":
                    return 0.0;
                default:
                    throw new IllegalArgumentException("Unknown function: " + func);
            }
        }

        private double factorial(double n) {
            if (n < 0 || n != Math.floor(n) || n > 170) {
                throw new ArithmeticException("Invalid input");
            }
            long val = (long) n;
            double res = 1.0;
            for (long i = 2; i <= val; i++) {
                res *= i;
            }
            return res;
        }

        private boolean match(TokenType... types) {
            for (TokenType type : types) {
                if (check(type)) {
                    advance();
                    return true;
                }
            }
            return false;
        }

        private boolean check(TokenType type) {
            if (isAtEnd()) return false;
            return peek().type == type;
        }

        private Token advance() {
            if (!isAtEnd()) current++;
            return previous();
        }

        private boolean isAtEnd() {
            return peek().type == TokenType.EOF;
        }

        private Token peek() {
            return tokens.get(current);
        }

        private Token previous() {
            return tokens.get(current - 1);
        }

        private Token consume(TokenType type, String message) {
            if (check(type)) return advance();
            throw new IllegalArgumentException(message);
        }
    }

    public static void main(String[] args) {
        String[] testCases = {
            "7 + 5",
            "12 \\times 3",
            "15 \\div 3",
            "5 \\div 2",
            "5 \\div 0",
            "5^2",
            "4^{-1}",
            "\\sqrt{16}",
            "\\sqrt{25 + 144}",
            "200 + 10\\%",
            "200 - 15\\%",
            "50\\%",
            "(-5) + 3",
            "2 + 3 \\times 4",
            "0.1 + 0.2",
            "100 \\div 4 + 5",
            "\\sin(90)"
        };

        for (String test : testCases) {
            EvaluationResult res = evaluate(test);
            System.out.println(test + " => " + (res.isSuccess() ? res.getFormattedResult() : "ERROR: " + res.getErrorMessage()));
        }
    }
}
