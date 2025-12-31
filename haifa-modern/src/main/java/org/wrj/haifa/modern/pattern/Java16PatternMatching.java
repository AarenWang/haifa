package org.wrj.modern.pattern;

/**
 * Java 16: instanceof 模式匹配
 *
 * JEP 394: Pattern Matching for instanceof
 * 引入了 instanceof 模式，消除了重复的类型转换
 */
public class Java16PatternMatching {

    /**
     * 基本的 instanceof 模式
     * 
     * Java 11 风格：
     * if (obj instanceof String) {
     *     String s = (String) obj;
     *     // 使用 s
     * }
     * 
     * Java 16 风格：
     * if (obj instanceof String s) {
     *     // 直接使用 s，自动转换
     * }
     */
    public static void basicPattern() {
        System.out.println("=== Java 16: Basic Pattern ===");
        
        Object[] objects = {"Hello", 42, 3.14, true, null};
        
        for (Object obj : objects) {
            if (obj instanceof String s) {
                System.out.println("String: " + s + " (length: " + s.length() + ")");
            } else if (obj instanceof Integer i) {
                System.out.println("Integer: " + i);
            } else if (obj instanceof Double d) {
                System.out.println("Double: " + d);
            } else if (obj instanceof Boolean b) {
                System.out.println("Boolean: " + b);
            } else if (obj == null) {
                System.out.println("Null value");
            } else {
                System.out.println("Unknown type");
            }
        }
        System.out.println();
    }

    /**
     * 模式与逻辑运算符的结合
     */
    public static void patternWithLogic() {
        System.out.println("=== Pattern with Logic ===");
        
        Object[] objects = {
            "Short",
            "This is a very long string",
            "",
            "   ",
            123,
            "test"
        };
        
        for (Object obj : objects) {
            // 模式与逻辑 AND
            if (obj instanceof String s && s.length() > 10) {
                System.out.println("长字符串: " + s);
            }
            
            // 模式与逻辑 AND (多个条件)
            if (obj instanceof String s && !s.isBlank() && s.contains("is")) {
                System.out.println("包含 'is' 的非空字符串: " + s);
            }
            
            // 模式与逻辑 OR
            if (obj instanceof String s || obj instanceof Integer i) {
                System.out.println("String or Integer: " + obj);
            }
        }
        System.out.println();
    }

    /**
     * 模式的作用域
     * 变量只在该分支的作用域内有效
     */
    public static void patternScope() {
        System.out.println("=== Pattern Scope ===");
        
        Object obj1 = "Hello";
        Object obj2 = 42;
        
        if (obj1 instanceof String s) {
            System.out.println("In if block: " + s);
            // s 只在此作用域内有效
        }
        // System.out.println(s); // 编译错误！s 不在此作用域
        
        if (!(obj2 instanceof String s)) {
            System.out.println("Not a string");
            // 这里 s 也不可用（逻辑否定）
        }
        
        System.out.println();
    }

    /**
     * 嵌套类型检查
     */
    public static void nestedTypeChecking() {
        System.out.println("=== Nested Type Checking ===");
        
        Object obj = java.util.List.of("a", "b", "c");
        
        // 检查是否是 List 的实例
        if (obj instanceof java.util.List<?> list) {
            System.out.println("List size: " + list.size());
            
            // 进一步检查元素类型
            if (!list.isEmpty() && list.get(0) instanceof String s) {
                System.out.println("First element is String: " + s);
            }
        }
        System.out.println();
    }

    /**
     * 数据验证模式
     */
    public static void validationPattern() {
        System.out.println("=== Validation Pattern ===");
        
        Object[] data = {
            "user@example.com",
            "invalid-email",
            "admin@company.org",
            123
        };
        
        for (Object obj : data) {
            if (obj instanceof String email && email.contains("@")) {
                System.out.println("Valid email: " + email);
            } else if (obj instanceof String s) {
                System.out.println("Invalid email: " + s);
            } else {
                System.out.println("Not a string: " + obj);
            }
        }
        System.out.println();
    }

    /**
     * 与 sealed classes 结合（预视 Java 15+ 特性）
     */
    public static void processShape(Object shape) {
        System.out.println("=== Working with Different Types ===");
        
        if (shape instanceof String s && s.equals("circle")) {
            System.out.println("Processing circle");
        } else if (shape instanceof String s && s.equals("square")) {
            System.out.println("Processing square");
        } else if (shape instanceof java.util.Map map) {
            System.out.println("Map with keys: " + map.keySet());
        }
        System.out.println();
    }

    /**
     * 性能优化：避免多次 instanceof 检查
     */
    public static void performanceExample() {
        System.out.println("=== Performance: Single Check vs Multiple ===");
        
        Object obj = "Example";
        
        // ❌ 不好：多次检查
        long startBad = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            if (obj instanceof String) {
                String s = (String) obj;
                // 使用 s
            }
        }
        long timeBad = System.nanoTime() - startBad;
        
        // ✅ 好：单次检查和转换
        long startGood = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            if (obj instanceof String s) {
                // 直接使用 s
            }
        }
        long timeGood = System.nanoTime() - startGood;
        
        System.out.println("Traditional way: " + timeBad + " ns");
        System.out.println("Pattern matching: " + timeGood + " ns");
        System.out.println();
    }

    public static void main(String[] args) {
        basicPattern();
        patternWithLogic();
        patternScope();
        nestedTypeChecking();
        validationPattern();
        processShape("circle");
        performanceExample();
    }
}
