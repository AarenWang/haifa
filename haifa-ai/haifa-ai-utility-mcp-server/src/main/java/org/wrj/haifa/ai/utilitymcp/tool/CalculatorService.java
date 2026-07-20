package org.wrj.haifa.ai.utilitymcp.tool;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.wrj.haifa.ai.utilitymcp.mcp.ToolArguments;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityResult;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;

public class CalculatorService {

    private static final int MAX_EXPRESSION = 512;
    private static final int MAX_DEPTH = 32;
    private static final int MAX_STEPS = 1_000;
    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_EVEN);

    public UtilityResult calculate(Map<String, Object> arguments) {
        String expression = new ToolArguments(arguments).requiredString("expression", MAX_EXPRESSION);
        Parser parser = new Parser(expression);
        BigDecimal value = parser.parse();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("expression", expression.replaceAll("\\s+", " ").trim());
        data.put("result", value.stripTrailingZeros().toPlainString());
        data.put("precision", MC.getPrecision());
        data.put("roundingMode", MC.getRoundingMode().name());
        return UtilityResult.local(data);
    }

    private static final class Parser {
        private final String input;
        private int position;
        private int depth;
        private int steps;

        private Parser(String input) { this.input = input; }

        private BigDecimal parse() {
            BigDecimal result = expression();
            skipWhitespace();
            if (position != input.length()) {
                throw invalid("unexpected token at position " + position);
            }
            return bounded(result);
        }

        private BigDecimal expression() {
            BigDecimal value = term();
            while (true) {
                if (consume('+')) value = bounded(value.add(term(), MC));
                else if (consume('-')) value = bounded(value.subtract(term(), MC));
                else return value;
            }
        }

        private BigDecimal term() {
            BigDecimal value = power();
            while (true) {
                if (consume('*')) value = bounded(value.multiply(power(), MC));
                else if (consume('/')) {
                    BigDecimal divisor = power();
                    if (divisor.signum() == 0) throw invalid("division by zero");
                    value = bounded(value.divide(divisor, MC));
                }
                else if (consume('%')) {
                    BigDecimal divisor = power();
                    if (divisor.signum() == 0) throw invalid("division by zero");
                    value = bounded(value.remainder(divisor, MC));
                }
                else return value;
            }
        }

        private BigDecimal power() {
            BigDecimal base = unary();
            if (consume('^')) {
                BigDecimal exponent = power();
                int intExponent;
                try { intExponent = exponent.intValueExact(); }
                catch (ArithmeticException ex) { throw invalid("exponent must be an integer"); }
                if (Math.abs(intExponent) > 1_000) throw invalid("exponent exceeds the supported range");
                if (intExponent >= 0) return bounded(base.pow(intExponent, MC));
                BigDecimal divisor = base.pow(-intExponent, MC);
                if (divisor.signum() == 0) throw invalid("division by zero");
                return bounded(BigDecimal.ONE.divide(divisor, MC));
            }
            return base;
        }

        private BigDecimal unary() {
            if (consume('+')) return unary();
            if (consume('-')) return bounded(unary().negate(MC));
            return primary();
        }

        private BigDecimal primary() {
            step();
            if (consume('(')) {
                if (++depth > MAX_DEPTH) throw invalid("expression nesting is too deep");
                BigDecimal value = expression();
                if (!consume(')')) throw invalid("missing closing parenthesis");
                depth--;
                return value;
            }
            skipWhitespace();
            if (position < input.length() && Character.isLetter(input.charAt(position))) {
                String identifier = identifier().toLowerCase(Locale.ROOT);
                if (identifier.equals("pi")) return new BigDecimal("3.141592653589793238462643383279503", MC);
                if (identifier.equals("e")) return new BigDecimal("2.718281828459045235360287471352662", MC);
                if (!consume('(')) throw invalid("unknown constant or function: " + identifier);
                BigDecimal first = expression();
                BigDecimal second = consume(',') ? expression() : null;
                if (!consume(')')) throw invalid("missing closing parenthesis for function");
                return function(identifier, first, second);
            }
            int start = position;
            boolean dot = false;
            while (position < input.length()) {
                char ch = input.charAt(position);
                if (Character.isDigit(ch)) position++;
                else if (ch == '.' && !dot) { dot = true; position++; }
                else break;
            }
            if (start == position) throw invalid("number expected at position " + position);
            try { return bounded(new BigDecimal(input.substring(start, position), MC)); }
            catch (NumberFormatException ex) { throw invalid("invalid number"); }
        }

        private BigDecimal function(String name, BigDecimal a, BigDecimal b) {
            return switch (name) {
                case "abs" -> bounded(a.abs(MC));
                case "min" -> requireSecond(name, a, b).min(a);
                case "max" -> requireSecond(name, a, b).max(a);
                case "pow" -> {
                    BigDecimal exponent = requireSecond(name, a, b);
                    int intExponent;
                    try { intExponent = exponent.intValueExact(); }
                    catch (ArithmeticException ex) { throw invalid("pow exponent must be an integer"); }
                    if (Math.abs(intExponent) > 1_000) throw invalid("pow exponent exceeds the supported range");
                    if (intExponent < 0 && a.signum() == 0) throw invalid("division by zero");
                    yield intExponent >= 0 ? bounded(a.pow(intExponent, MC))
                            : bounded(BigDecimal.ONE.divide(a.pow(-intExponent, MC), MC));
                }
                case "sqrt" -> {
                    if (a.signum() < 0) throw invalid("sqrt requires a non-negative value");
                    yield bounded(a.sqrt(MC));
                }
                case "round" -> a.setScale(0, RoundingMode.HALF_EVEN);
                case "floor" -> a.setScale(0, RoundingMode.FLOOR);
                case "ceil" -> a.setScale(0, RoundingMode.CEILING);
                default -> throw invalid("unsupported function: " + name);
            };
        }

        private static BigDecimal requireSecond(String name, BigDecimal a, BigDecimal b) {
            if (b == null) throw invalid(name + " requires two arguments");
            return b;
        }

        private String identifier() {
            int start = position;
            while (position < input.length() && Character.isLetter(input.charAt(position))) position++;
            return input.substring(start, position);
        }

        private boolean consume(char expected) {
            skipWhitespace();
            if (position < input.length() && input.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) position++;
        }

        private void step() {
            if (++steps > MAX_STEPS) throw invalid("expression exceeds the operation limit");
        }

        private static BigDecimal bounded(BigDecimal value) {
            if (value.precision() > 100 || value.scale() > 100 || value.abs().compareTo(new BigDecimal("1E100")) > 0) {
                throw invalid("result exceeds the supported precision or magnitude");
            }
            return value;
        }

        private static UtilityToolException invalid(String message) {
            return UtilityToolException.invalid(message);
        }
    }
}
