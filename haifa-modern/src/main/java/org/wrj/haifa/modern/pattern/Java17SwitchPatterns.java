package org.wrj.modern.pattern;

/**
 * Java 17: Switch 表达式与模式匹配
 *
 * JEP 406: Pattern Matching for switch (Preview in 17, Final in 21)
 * 将模式匹配扩展到 switch 表达式，提供更强大的表达能力
 */
public class Java17SwitchPatterns {

    /**
     * 基本的 switch 模式表达式
     * 
     * 特点：
     * - 使用 -> 语法，不需要 break
     * - 可以作为表达式返回值
     * - 编译器检查完整性
     */
    public static String basicSwitchPattern(Object obj) {
        System.out.println("=== Java 17: Basic Switch Pattern ===");
        
        String result = switch (obj) {
            case String s -> "String: " + s;
            case Integer i -> "Integer: " + i;
            case Double d -> "Double: " + d;
            case Boolean b -> "Boolean: " + b;
            case null -> "Null value";
            default -> "Unknown type";
        };
        
        System.out.println(result);
        System.out.println();
        return result;
    }

    /**
     * 使用守卫条件（guard）
     * 
     * 守卫条件允许在模式后面添加额外的条件
     * 只有当模式匹配且守卫条件为真时，才执行该分支
     */
    public static String switchWithGuards(Object obj) {
        System.out.println("=== Switch with Guard Conditions ===");
        
        String result = switch (obj) {
            case String s when s.length() > 10 -> "Long string: " + s;
            case String s when s.isEmpty() -> "Empty string";
            case String s -> "Short string: " + s;
            
            case Integer i when i < 0 -> "Negative integer: " + i;
            case Integer i when i == 0 -> "Zero";
            case Integer i -> "Positive integer: " + i;
            
            case Double d when d > 100.0 -> "Large double: " + d;
            case Double d -> "Small double: " + d;
            
            case null -> "Null value";
            default -> "Unknown";
        };
        
        System.out.println(result);
        System.out.println();
        return result;
    }

    /**
     * Switch 表达式的完整性检查
     * 
     * 编译器确保所有可能的情况都被处理
     */
    public static void completenessCheck() {
        System.out.println("=== Completeness Check ===");
        
        // 使用 sealed interface 来演示完整性检查
        Object[] objects = {
            "string",
            123,
            45.6,
            true,
            null
        };
        
        for (Object obj : objects) {
            // 以下代码是 exhaustive（完整的）
            String result = switch (obj) {
                case String s -> "S: " + s;
                case Integer i -> "I: " + i;
                case Double d -> "D: " + d;
                case Boolean b -> "B: " + b;
                case null -> "null";
                // 如果移除 default，编译器会报错
            };
            System.out.println(result);
        }
        System.out.println();
    }

    /**
     * 多个数据类型的处理
     */
    public static int processValue(Object value) {
        System.out.println("=== Process Multiple Types ===");
        
        int result = switch (value) {
            case String s -> s.length();
            case Integer i -> i;
            case Double d -> (int) d.doubleValue();
            case Boolean b -> b ? 1 : 0;
            case java.util.List list -> list.size();
            case java.util.Map map -> map.size();
            case null -> -1;
            default -> 0;
        };
        
        System.out.println("Result: " + result);
        System.out.println();
        return result;
    }

    /**
     * 复杂的条件逻辑
     */
    public static String complexLogic(Object obj) {
        System.out.println("=== Complex Logic ===");
        
        String result = switch (obj) {
            case String s when s.startsWith("http://") || s.startsWith("https://") ->
                "Valid URL: " + s;
            case String s when s.matches("[a-zA-Z0-9._%-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}") ->
                "Valid Email: " + s;
            case String s when s.length() >= 6 && s.length() <= 20 ->
                "Valid Username: " + s;
            case String s ->
                "Invalid input: " + s;
            
            case Integer i when i >= 0 && i <= 100 ->
                "Valid percentage: " + i + "%";
            case Integer i when i < 0 ->
                "Negative value: " + i;
            case Integer i ->
                "Out of range: " + i;
            
            default -> "Unknown type";
        };
        
        System.out.println(result);
        System.out.println();
        return result;
    }

    /**
     * 与 if-else-if 的对比
     */
    public static void comparisonWithIfElse() {
        System.out.println("=== Comparison: Switch vs If-Else ===");
        
        Object obj = "Hello World";
        
        // 旧风格：if-else-if
        String resultIfElse;
        if (obj instanceof String) {
            String s = (String) obj;
            if (s.length() > 5) {
                resultIfElse = "Long: " + s;
            } else {
                resultIfElse = "Short: " + s;
            }
        } else if (obj instanceof Integer) {
            resultIfElse = "Integer: " + obj;
        } else {
            resultIfElse = "Unknown";
        }
        
        // 新风格：switch 表达式
        String resultSwitch = switch (obj) {
            case String s when s.length() > 5 -> "Long: " + s;
            case String s -> "Short: " + s;
            case Integer i -> "Integer: " + i;
            default -> "Unknown";
        };
        
        System.out.println("If-else result: " + resultIfElse);
        System.out.println("Switch result: " + resultSwitch);
        System.out.println();
    }

    /**
     * 类型和守卫的结合
     */
    public static String categorizeObject(Object obj) {
        System.out.println("=== Type and Guard Combination ===");
        
        String category = switch (obj) {
            // 类型模式与守卫
            case String s when s.length() == 0 -> "empty-string";
            case String s when Character.isUpperCase(s.charAt(0)) -> "capitalized-string";
            case String s -> "lowercase-string";
            
            // 数值比较
            case Number n when n.doubleValue() > 0 -> "positive-number";
            case Number n when n.doubleValue() < 0 -> "negative-number";
            case Number n -> "zero";
            
            // Null 处理
            case null -> "null-value";
            
            default -> "other";
        };
        
        System.out.println("Category: " + category);
        System.out.println();
        return category;
    }

    /**
     * Switch 作为表达式的优势
     */
    public static void switchAsExpression() {
        System.out.println("=== Switch as Expression ===");
        
        Object[] data = {"test", 100, 3.14, null};
        
        for (Object item : data) {
            // 直接在表达式中使用 switch
            System.out.println(
                "Item " + item + " -> " + 
                switch (item) {
                    case String s -> "String (" + s.length() + " chars)";
                    case Number n -> "Number (" + n + ")";
                    case null -> "null";
                    default -> "other";
                }
            );
        }
        System.out.println();
    }

    /**
     * 实际应用：API 响应处理
     */
    public static String handleApiResponse(Object response) {
        System.out.println("=== API Response Handling ===");
        
        return switch (response) {
            case String s when s.startsWith("ERROR:") ->
                "Error response: " + s.substring(6);
            case String s when s.startsWith("OK:") ->
                "Success: " + s.substring(3);
            case java.util.Map map when map.containsKey("status") ->
                "Map response with status: " + map.get("status");
            case java.util.List list when list.size() > 0 ->
                "List with " + list.size() + " items";
            case null ->
                "Empty response";
            default ->
                "Unknown response format";
        };
    }

    public static void main(String[] args) {
        basicSwitchPattern("Hello");
        basicSwitchPattern(42);
        basicSwitchPattern(null);
        
        switchWithGuards("This is a long string");
        switchWithGuards("");
        switchWithGuards(123);
        switchWithGuards(-5);
        
        completenessCheck();
        
        processValue("Java");
        processValue(100);
        processValue(99.9);
        processValue(true);
        processValue(java.util.List.of(1, 2, 3));
        
        complexLogic("https://example.com");
        complexLogic("user@email.com");
        complexLogic("abc");
        complexLogic(50);
        
        comparisonWithIfElse();
        
        categorizeObject("");
        categorizeObject("Hello");
        categorizeObject("hello");
        
        switchAsExpression();
        
        System.out.println(handleApiResponse("ERROR: Connection failed"));
        System.out.println(handleApiResponse("OK: Data retrieved"));
        System.out.println(handleApiResponse(java.util.Map.of("status", "success")));
    }
}
