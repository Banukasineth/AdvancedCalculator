package com.banukasineth.advancedcalculator.app.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * - Standard trigonometric functions (sin, cos, tan, sec, csc, cot, asin, acos, atan)
 * - Hyperbolic functions (sinh, cosh, tanh, sech, csch, coth)
 * - Logarithms and exponentials (log, ln, exp, 10^x)
 * - Absolute value (abs, |x|)
 * - Programmer mode bitwise and multi-base integer evaluation (HEX, DEC, OCT, BIN)
 * - Clean number formatting avoiding IEEE 754 precision artifacts
 */
public class MathEvaluator {

    public static boolean isDegreeMode = true;

    public static class EvaluationResult {
        private final boolean success;
        private final double value;
        private final String formattedResult;
        private final String errorMessage;
        private List<String> steps;

        private EvaluationResult(double value, String formattedResult) {
            this.success = true;
            this.value = value;
            this.formattedResult = formattedResult;
            this.errorMessage = null;
            this.steps = new ArrayList<>();
        }

        private EvaluationResult(String errorMessage) {
            this.success = false;
            this.value = Double.NaN;
            this.formattedResult = errorMessage;
            this.errorMessage = errorMessage;
            this.steps = new ArrayList<>();
        }

        public static EvaluationResult ok(double value) {
            return new EvaluationResult(value, formatNumber(value));
        }

        public static EvaluationResult ok(double value, String formattedResult) {
            return new EvaluationResult(value, formattedResult);
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

        public List<String> getSteps() {
            return steps;
        }

        public void setSteps(List<String> steps) {
            this.steps = steps;
        }

        public String getFormattedAlignedLatex(String rawExpr) {
            List<String> stepList = (steps != null && !steps.isEmpty()) ? steps : generateSteps(rawExpr, formattedResult);
            if (stepList.isEmpty()) {
                return formattedResult;
            }
            if (stepList.size() == 1) {
                return stepList.get(0);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("\\begin{aligned}\n");
            sb.append("& ").append(stepList.get(0)).append(" \\\\\n");
            for (int i = 1; i < stepList.size(); i++) {
                sb.append("&= ").append(stepList.get(i));
                if (i < stepList.size() - 1) {
                    sb.append(" \\\\\n");
                } else {
                    sb.append("\n");
                }
            }
            sb.append("\\end{aligned}");
            return sb.toString();
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
            String formatted = formatNumber(result);
            EvaluationResult evalRes = EvaluationResult.ok(result, formatted);
            evalRes.setSteps(generateSteps(rawExpression, formatted));
            return evalRes;
        } catch (ArithmeticException ex) {
            return EvaluationResult.error(ex.getMessage());
        } catch (Exception ex) {
            return EvaluationResult.error("Invalid expression");
        }
    }

    public static List<String> generateSteps(String rawExpression, String finalResult) {
        List<String> steps = new ArrayList<>();
        if (rawExpression == null || rawExpression.trim().isEmpty()) {
            return steps;
        }

        String current = rawExpression.trim();
        if (current.endsWith("=")) {
            current = current.substring(0, current.length() - 1).trim();
        }

        steps.add(current);

        if (isSingleNumber(current) || current.equals(finalResult)) {
            return steps;
        }

        try {
            int maxSteps = 8;
            while (maxSteps-- > 0) {
                String next = reduceOneStep(current);
                if (next == null || next.trim().equals(current.trim()) || next.trim().equals(finalResult.trim())) {
                    break;
                }
                if (!next.trim().equals(steps.get(steps.size() - 1).trim())) {
                    steps.add(next.trim());
                }
                current = next.trim();
            }
        } catch (Exception ignored) {
        }

        if (steps.isEmpty() || !steps.get(steps.size() - 1).trim().equals(finalResult.trim())) {
            steps.add(finalResult.trim());
        }

        return steps;
    }

    private static boolean isSingleNumber(String s) {
        if (s == null) return false;
        return s.trim().matches("^-?[0-9]+(?:\\.[0-9]+)?$");
    }

    private static String reduceOneStep(String expr) {
        if (expr == null || expr.trim().isEmpty()) return null;
        String s = expr.trim();

        // 1. Parentheses: find innermost parentheses
        Pattern pParen = Pattern.compile("\\(([^()]+)\\)");
        Matcher mParen = pParen.matcher(s);
        if (mParen.find()) {
            String inner = mParen.group(1).trim();
            if (isSingleNumber(inner)) {
                return s.substring(0, mParen.start()) + inner + s.substring(mParen.end());
            }
            String reducedInner = reduceOneStep(inner);
            if (reducedInner != null && !reducedInner.equals(inner)) {
                if (isSingleNumber(reducedInner)) {
                    return s.substring(0, mParen.start()) + reducedInner + s.substring(mParen.end());
                }
                return s.substring(0, mParen.start()) + "(" + reducedInner + ")" + s.substring(mParen.end());
            } else {
                try {
                    EvaluationResult innerRes = evaluate(inner);
                    if (innerRes.isSuccess()) {
                        return s.substring(0, mParen.start()) + innerRes.getFormattedResult() + s.substring(mParen.end());
                    }
                } catch (Exception ignored) {}
            }
        }

        // 2. Square roots: \sqrt{N} or \sqrt(N)
        Pattern pSqrt = Pattern.compile("\\\\sqrt\\{([0-9.]+)\\}|\\\\sqrt\\(([0-9.]+)\\)");
        Matcher mSqrt = pSqrt.matcher(s);
        if (mSqrt.find()) {
            String numStr = mSqrt.group(1) != null ? mSqrt.group(1) : mSqrt.group(2);
            double val = Math.sqrt(Double.parseDouble(numStr));
            return s.substring(0, mSqrt.start()) + formatNumber(val) + s.substring(mSqrt.end());
        }

        // 3. Powers: N^{M} or N^M or N^2
        Pattern pPow = Pattern.compile("(-?[0-9.]+)\\^\\{?([0-9.]+)\\}?|(-?[0-9.]+)\\^2");
        Matcher mPow = pPow.matcher(s);
        if (mPow.find()) {
            if (mPow.group(3) != null) {
                double b = Double.parseDouble(mPow.group(3));
                return s.substring(0, mPow.start()) + formatNumber(b * b) + s.substring(mPow.end());
            } else {
                double b = Double.parseDouble(mPow.group(1));
                double e = Double.parseDouble(mPow.group(2));
                return s.substring(0, mPow.start()) + formatNumber(Math.pow(b, e)) + s.substring(mPow.end());
            }
        }

        // 4. Factorial: N!
        Pattern pFact = Pattern.compile("([0-9]+)!");
        Matcher mFact = pFact.matcher(s);
        if (mFact.find()) {
            long n = Long.parseLong(mFact.group(1));
            return s.substring(0, mFact.start()) + formatNumber((double) factorial(n)) + s.substring(mFact.end());
        }

        // 5. Trig functions
        Pattern pTrig = Pattern.compile("\\\\(sin|cos|tan|cot|sec|csc)\\s*(?:\\{([0-9.]+)\\}|\\(([0-9.]+)\\))");
        Matcher mTrig = pTrig.matcher(s);
        if (mTrig.find()) {
            String func = mTrig.group(1);
            String numStr = mTrig.group(2) != null ? mTrig.group(2) : mTrig.group(3);
            double v = Double.parseDouble(numStr);
            double res = evalTrig(func, v, isDegreeMode);
            return s.substring(0, mTrig.start()) + formatNumber(res) + s.substring(mTrig.end());
        }

        // 6. Absolute value: |N|
        Pattern pAbs = Pattern.compile("\\|([0-9.]+)\\|");
        Matcher mAbs = pAbs.matcher(s);
        if (mAbs.find()) {
            double v = Math.abs(Double.parseDouble(mAbs.group(1)));
            return s.substring(0, mAbs.start()) + formatNumber(v) + s.substring(mAbs.end());
        }

        // 7. Multiplication, Division, Modulo
        Pattern pMulDiv = Pattern.compile("(-?[0-9.]+(?:[eE][+-]?[0-9]+)?)\\s*(\\\\times|\\\\div|\\\\bmod|\\*|\\/|%)\\s*(-?[0-9.]+(?:[eE][+-]?[0-9]+)?)");
        Matcher mMulDiv = pMulDiv.matcher(s);
        if (mMulDiv.find()) {
            try {
                double left = Double.parseDouble(mMulDiv.group(1));
                String op = mMulDiv.group(2);
                double right = Double.parseDouble(mMulDiv.group(3));
                double res = 0;
                if (op.equals("\\times") || op.equals("*")) {
                    res = left * right;
                } else if (op.equals("\\div") || op.equals("/")) {
                    if (right == 0) return null;
                    res = left / right;
                } else if (op.equals("\\bmod") || op.equals("%")) {
                    if (right == 0) return null;
                    res = left % right;
                }
                return s.substring(0, mMulDiv.start()) + formatNumber(res) + s.substring(mMulDiv.end());
            } catch (Exception ignored) {}
        }

        // 8. Addition and Subtraction
        Pattern pAddSub = Pattern.compile("(?<=^|[^0-9.])(-?[0-9.]+(?:[eE][+-]?[0-9]+)?)\\s*([+\\-])\\s*([0-9.]+(?:[eE][+-]?[0-9]+)?)");
        Matcher mAddSub = pAddSub.matcher(s);
        if (mAddSub.find()) {
            try {
                double left = Double.parseDouble(mAddSub.group(1));
                String op = mAddSub.group(2);
                double right = Double.parseDouble(mAddSub.group(3));
                double res = op.equals("+") ? (left + right) : (left - right);
                return s.substring(0, mAddSub.start()) + formatNumber(res) + s.substring(mAddSub.end());
            } catch (Exception ignored) {}
        }

        return null;
    }

    private static double evalTrig(String func, double val, boolean deg) {
        double rad = deg ? Math.toRadians(val) : val;
        switch (func.toLowerCase()) {
            case "sin": return Math.sin(rad);
            case "cos": return Math.cos(rad);
            case "tan": return Math.tan(rad);
            case "cot": return 1.0 / Math.tan(rad);
            case "sec": return 1.0 / Math.cos(rad);
            case "csc": return 1.0 / Math.sin(rad);
            default: return val;
        }
    }

    private static long factorial(long n) {
        if (n < 0 || n > 20) return 0;
        long f = 1;
        for (long i = 2; i <= n; i++) f *= i;
        return f;
    }

    /**
     * Converts a number string from one base to another (e.g. DEC to HEX or BIN).
     */
    public static String convertBase(String numStr, int fromBase, int toBase) {
        if (numStr == null || numStr.trim().isEmpty()) return "0";
        try {
            String clean = numStr.trim().replace(" ", "").toUpperCase();
            boolean neg = clean.startsWith("-");
            if (neg) clean = clean.substring(1);
            long val = Long.parseUnsignedLong(clean, fromBase);
            String res;
            switch (toBase) {
                case 16: res = Long.toHexString(val).toUpperCase(); break;
                case 8:  res = Long.toOctalString(val); break;
                case 2:  res = Long.toBinaryString(val); break;
                default: res = Long.toString(val); break;
            }
            return neg ? "-" + res : res;
        } catch (Exception e) {
            return numStr;
        }
    }

    /**
     * Evaluates integer arithmetic and bitwise expressions for Programmer mode in the specified base.
     */
    public static EvaluationResult evaluateProgrammer(String rawExpression, int base) {
        if (rawExpression == null || rawExpression.trim().isEmpty()) {
            return EvaluationResult.error("Empty expression");
        }
        try {
            String s = rawExpression.trim()
                .replace("\\operatorname{AND}", "&")
                .replace("\\operatorname{OR}", "|")
                .replace("\\operatorname{XOR}", "^")
                .replace("\\times", "*")
                .replace("\\div", "/")
                .replace("\\bmod", "%")
                .replace("AND", "&")
                .replace("OR", "|")
                .replace("XOR", "^");

            ProgrammerParser parser = new ProgrammerParser(s, base);
            long val = parser.parse();
            String formatted;
            switch (base) {
                case 16: formatted = Long.toHexString(val).toUpperCase(); break;
                case 8:  formatted = Long.toOctalString(val); break;
                case 2:  formatted = Long.toBinaryString(val); break;
                default: formatted = Long.toString(val); break;
            }
            EvaluationResult evalRes = EvaluationResult.ok((double) val, formatted);
            List<String> steps = new ArrayList<>();
            steps.add(rawExpression.trim());
            String hexVal = Long.toHexString(val).toUpperCase();
            String decVal = Long.toString(val);
            String octVal = Long.toOctalString(val);
            String binVal = Long.toBinaryString(val);

            if (base != 10) {
                steps.add("\\text{DEC: } " + decVal);
            }
            steps.add(formatted);
            steps.add("\\text{\\small [HEX: " + hexVal + " \\quad DEC: " + decVal + " \\quad OCT: " + octVal + " \\quad BIN: " + binVal + "]}");
            evalRes.setSteps(steps);
            return evalRes;
        } catch (ArithmeticException e) {
            return EvaluationResult.error(e.getMessage());
        } catch (Exception e) {
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
             .replace("\\operatorname{Re}", "re")
             .replace("\\operatorname{Im}", "im")
             .replace("\\operatorname{AND}", "&")
             .replace("\\operatorname{OR}", "|")
             .replace("^{\\circ}", "")
             .replace("\\text{rad}", "");

        // Remove subscripts: _{n} or _{...}
        s = s.replaceAll("_\\{[^}]*\\}", "");

        // Operators
        s = s.replace("\\times", "*")
             .replace("\\cdot", "*")
             .replace("\\div", "/")
             .replace("\\bmod", " mod ")
             .replace("\\%", "%");

        // Square roots: \sqrt{...} -> sqrt(...)
        s = s.replaceAll("\\\\sqrt\\s*\\{", "sqrt(")
             .replaceAll("\\\\sqrt\\s*\\(", "sqrt(");

        // Exponents: ^{...} -> ^(...)
        s = s.replaceAll("\\^\\s*\\{", "^(")
             .replaceAll("\\^\\s*2", "^(2)");

        // 10^x power
        s = s.replaceAll("10\\^\\s*\\{", "10^(")
             .replaceAll("10\\^\\s*\\(", "10^(");

        // Pipe absolute values: |expr| -> abs(expr)
        while (s.matches(".*\\|[^|]+\\|.*")) {
            s = s.replaceAll("\\|([^|]+)\\|", "abs($1)");
        }

        // Functions: \sin, \cos, \tan, \exp, etc.
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
    // Tokenizer & Parser for Standard/Scientific
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

                if (word.equalsIgnoreCase("mod")) {
                    tokens.add(new Token(TokenType.MOD, "mod"));
                    continue;
                }

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
                    return isDegreeMode ? Math.sin(Math.toRadians(arg)) : Math.sin(arg);
                case "cos":
                    return isDegreeMode ? Math.cos(Math.toRadians(arg)) : Math.cos(arg);
                case "tan":
                    if (isDegreeMode) {
                        double mod = Math.abs(arg) % 180;
                        if (Math.abs(mod - 90) < 1e-9) throw new ArithmeticException("Invalid input");
                        return Math.tan(Math.toRadians(arg));
                    } else {
                        return Math.tan(arg);
                    }
                case "sec": {
                    double cVal = isDegreeMode ? Math.cos(Math.toRadians(arg)) : Math.cos(arg);
                    if (Math.abs(cVal) < 1e-12) throw new ArithmeticException("Invalid input");
                    return 1.0 / cVal;
                }
                case "csc": {
                    double sVal = isDegreeMode ? Math.sin(Math.toRadians(arg)) : Math.sin(arg);
                    if (Math.abs(sVal) < 1e-12) throw new ArithmeticException("Invalid input");
                    return 1.0 / sVal;
                }
                case "cot": {
                    double tVal = isDegreeMode ? Math.tan(Math.toRadians(arg)) : Math.tan(arg);
                    if (Math.abs(tVal) < 1e-12) throw new ArithmeticException("Invalid input");
                    return 1.0 / tVal;
                }
                case "asin":
                    if (arg < -1 || arg > 1) throw new ArithmeticException("Invalid input");
                    return isDegreeMode ? Math.toDegrees(Math.asin(arg)) : Math.asin(arg);
                case "acos":
                    if (arg < -1 || arg > 1) throw new ArithmeticException("Invalid input");
                    return isDegreeMode ? Math.toDegrees(Math.acos(arg)) : Math.acos(arg);
                case "atan":
                    return isDegreeMode ? Math.toDegrees(Math.atan(arg)) : Math.atan(arg);
                case "sinh":
                    return Math.sinh(arg);
                case "cosh":
                    return Math.cosh(arg);
                case "tanh":
                    return Math.tanh(arg);
                case "sech":
                    return 1.0 / Math.cosh(arg);
                case "csch":
                    if (arg == 0) throw new ArithmeticException("Cannot divide by zero");
                    return 1.0 / Math.sinh(arg);
                case "coth":
                    if (arg == 0) throw new ArithmeticException("Cannot divide by zero");
                    return 1.0 / Math.tanh(arg);
                case "exp":
                    return Math.exp(arg);
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

    // ==========================================
    // Parser for Programmer Mode (Hex, Dec, Oct, Bin)
    // ==========================================

    private static class ProgrammerParser {
        private final String input;
        private final int base;
        private int pos = 0;

        ProgrammerParser(String input, int base) {
            this.input = input;
            this.base = base;
        }

        long parse() {
            long res = parseOr();
            skipWhitespace();
            if (pos < input.length()) {
                throw new IllegalArgumentException("Unexpected character: " + input.charAt(pos));
            }
            return res;
        }

        private void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++;
        }

        private boolean matchChar(char expected) {
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == expected) {
                pos++;
                return true;
            }
            return false;
        }

        private long parseOr() {
            long left = parseXor();
            while (matchChar('|')) {
                left |= parseXor();
            }
            return left;
        }

        private long parseXor() {
            long left = parseAnd();
            while (matchChar('^')) {
                left ^= parseAnd();
            }
            return left;
        }

        private long parseAnd() {
            long left = parseAddSub();
            while (matchChar('&')) {
                left &= parseAddSub();
            }
            return left;
        }

        private long parseAddSub() {
            long left = parseMulDiv();
            while (true) {
                if (matchChar('+')) {
                    left += parseMulDiv();
                } else if (matchChar('-')) {
                    left -= parseMulDiv();
                } else {
                    break;
                }
            }
            return left;
        }

        private long parseMulDiv() {
            long left = parseUnary();
            while (true) {
                if (matchChar('*')) {
                    left *= parseUnary();
                } else if (matchChar('/')) {
                    long right = parseUnary();
                    if (right == 0) throw new ArithmeticException("Cannot divide by zero");
                    left /= right;
                } else if (matchChar('%')) {
                    long right = parseUnary();
                    if (right == 0) throw new ArithmeticException("Cannot divide by zero");
                    left %= right;
                } else {
                    break;
                }
            }
            return left;
        }

        private long parseUnary() {
            skipWhitespace();
            if (matchChar('-')) {
                return -parseUnary();
            }
            if (matchChar('~')) {
                return ~parseUnary();
            }
            return parsePrimary();
        }

        private long parsePrimary() {
            skipWhitespace();
            if (matchChar('(')) {
                long val = parseOr();
                if (!matchChar(')')) throw new IllegalArgumentException("Missing ')'");
                return val;
            }

            int start = pos;
            while (pos < input.length() && isBaseDigit(input.charAt(pos), base)) {
                pos++;
            }
            if (pos == start) {
                throw new IllegalArgumentException("Expected number at position " + pos);
            }
            String numStr = input.substring(start, pos);
            return Long.parseUnsignedLong(numStr, base);
        }

        private boolean isBaseDigit(char c, int base) {
            c = Character.toUpperCase(c);
            if (base == 2) return c == '0' || c == '1';
            if (base == 8) return c >= '0' && c <= '7';
            if (base == 10) return c >= '0' && c <= '9';
            if (base == 16) return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F');
            return false;
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
            "\\sin(90)",
            "\\cos(0)",
            "\\sec(0)",
            "\\sinh(0)",
            "\\exp(1)",
            "|-15|",
            "5!",
            "10^{3}",
            "5 \\bmod 2"
        };

        for (String test : testCases) {
            EvaluationResult res = evaluate(test);
            System.out.println(test + " => " + (res.isSuccess() ? res.getFormattedResult() : "ERROR: " + res.getErrorMessage()));
        }

        System.out.println("\n-- Degree vs Radian Tests --");
        isDegreeMode = true;
        System.out.println("Degree mode sin(90) => " + evaluate("\\sin(90)").getFormattedResult());
        isDegreeMode = false;
        System.out.println("Radian mode sin(pi / 2) => " + evaluate("\\sin(\\pi / 2)").getFormattedResult());
        isDegreeMode = true; // reset to default

        System.out.println("\n-- Programmer Mode Tests --");
        System.out.println("HEX A + B => " + evaluateProgrammer("A + B", 16).getFormattedResult());
        System.out.println("HEX F \\operatorname{AND} 7 => " + evaluateProgrammer("F \\operatorname{AND} 7", 16).getFormattedResult());
        System.out.println("HEX 8 \\operatorname{OR} 4 => " + evaluateProgrammer("8 \\operatorname{OR} 4", 16).getFormattedResult());
        System.out.println("BIN 1010 + 0101 => " + evaluateProgrammer("1010 + 0101", 2).getFormattedResult());
        System.out.println("OCT 7 + 1 => " + evaluateProgrammer("7 + 1", 8).getFormattedResult());
        System.out.println("Convert 16 from DEC to HEX => " + convertBase("16", 10, 16));
        System.out.println("Convert 16 from DEC to BIN => " + convertBase("16", 10, 2));

        System.out.println("\n-- Step Generation Tests --");
        String[] stepTests = {"2 + 3", "5 + 3 \\times 2", "\\sqrt{16} + 5", "(4 + 2) \\times 3 - 5", "10 + 20 + 30"};
        for (String st : stepTests) {
            EvaluationResult r = evaluate(st);
            System.out.println("Input: " + st);
            System.out.println("Aligned LaTeX:\n" + r.getFormattedAlignedLatex(st));
            System.out.println("-------------");
        }
    }
}
