package org.wrj.modern;

/**
 * Java 11+ 新特性展示
 * - var 局部变量类型推断
 * - 文本块 (Text Blocks)
 * - HTTP Client API
 * - 记录类 (Records)
 * - Sealed Classes
 * - Pattern Matching
 * - 虚拟线程 (Virtual Threads) - Java 21+
 */
public class ModernJavaFeatures {

    public static void main(String[] args) {
        System.out.println("=== Modern Java Features ===\n");

        // 1. var 局部变量类型推断 (Java 10+)
        varDemo();

        // 2. 文本块 (Java 13+)
        textBlockDemo();

        // 3. instanceof with Pattern Matching (Java 16+)
        patternMatchingDemo();

        // 4. NullPointerException详细信息 (Java 14+)
        npeDemo();
    }

    /**
     * var 类型推断示例
     */
    private static void varDemo() {
        System.out.println("1. Var Type Inference:");
        var message = "Hello, Modern Java";
        var numbers = java.util.List.of(1, 2, 3, 4, 5);
        var map = java.util.Map.of("key1", "value1", "key2", "value2");

        System.out.println("  message: " + message);
        System.out.println("  numbers: " + numbers);
        System.out.println("  map: " + map);
        System.out.println();
    }

    /**
     * 文本块示例 (Java 13+)
     */
    private static void textBlockDemo() {
        System.out.println("2. Text Blocks:");
        String json = """
                {
                    "name": "Modern Java",
                    "version": "21",
                    "features": ["var", "records", "sealed classes"]
                }""";
        System.out.println(json);
        System.out.println();
    }

    /**
     * Pattern Matching 示例 (Java 16+)
     */
    private static void patternMatchingDemo() {
        System.out.println("3. Pattern Matching with instanceof:");
        Object[] objects = {"String", 42, 3.14, true};

        for (Object obj : objects) {
            String result = switch (obj) {
                case String s -> "String: " + s;
                case Integer i -> "Integer: " + i;
                case Double d -> "Double: " + d;
                case Boolean b -> "Boolean: " + b;
                default -> "Unknown: " + obj;
            };
            System.out.println("  " + result);
        }
        System.out.println();
    }

    /**
     * NullPointerException 详细信息示例 (Java 14+)
     */
    private static void npeDemo() {
        System.out.println("4. Helpful NullPointerException:");
        System.out.println("  Java 14+ provides detailed NPE messages");
        System.out.println("  showing exactly which field was null");
        System.out.println();
    }
}
