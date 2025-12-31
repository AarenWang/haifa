package org.wrj.modern.pattern;

import org.junit.Test;

/**
 * 模式匹配测试套件
 */
public class PatternMatchingTest {

    // ============ Java 16 instanceof Pattern Tests ============
    
    @Test
    public void testBasicInstanceofPattern() {
        Object[] objects = {"Hello", 42, 3.14};
        
        for (Object obj : objects) {
            if (obj instanceof String s) {
                assert s.length() > 0;
            } else if (obj instanceof Integer i) {
                assert i > 0;
            } else if (obj instanceof Double d) {
                assert d > 0;
            }
        }
    }

    @Test
    public void testPatternWithLogicalOperators() {
        Object str = "This is a long string";
        
        if (str instanceof String s && s.length() > 10) {
            assert s.contains("long");
        }
    }

    @Test
    public void testPatternScope() {
        Object obj = "test";
        
        if (obj instanceof String s) {
            assert s.equals("test");
        }
        // s is not accessible here
    }

    // ============ Java 17 Switch Pattern Tests ============
    
    @Test
    public void testBasicSwitchPattern() {
        Object obj = "Hello";
        
        String result = switch (obj) {
            case String s -> "String";
            case Integer i -> "Integer";
            case null -> "Null";
            default -> "Other";
        };
        
        assert result.equals("String");
    }

    @Test
    public void testSwitchWithGuards() {
        Object obj = 15;
        
        String result = switch (obj) {
            case Integer i when i > 10 -> "Greater than 10";
            case Integer i when i < 10 -> "Less than 10";
            case Integer i -> "Equal to 10";
            default -> "Not an integer";
        };
        
        assert result.equals("Greater than 10");
    }

    @Test
    public void testSwitchWithNull() {
        Object obj = null;
        
        String result = switch (obj) {
            case String s -> "String";
            case null -> "Null";
            default -> "Other";
        };
        
        assert result.equals("Null");
    }

    @Test
    public void testSwitchExhaustiveness() {
        for (Object obj : new Object[]{"a", 1, 2.5, true, null}) {
            String result = switch (obj) {
                case String s -> "S";
                case Integer i -> "I";
                case Double d -> "D";
                case Boolean b -> "B";
                case null -> "N";
            };
            assert result != null;
        }
    }

    // ============ Java 19 Record Pattern Tests ============
    
    @Test
    public void testBasicRecordPattern() {
        Java19RecordPatterns.Point p = new Java19RecordPatterns.Point(10, 20);
        
        if (p instanceof Java19RecordPatterns.Point(int x, int y)) {
            assert x == 10;
            assert y == 20;
        }
    }

    @Test
    public void testNestedRecordPattern() {
        Java19RecordPatterns.Point p1 = new Java19RecordPatterns.Point(0, 0);
        Java19RecordPatterns.Point p2 = new Java19RecordPatterns.Point(10, 10);
        Java19RecordPatterns.Rectangle rect = new Java19RecordPatterns.Rectangle(p1, p2);
        
        if (rect instanceof Java19RecordPatterns.Rectangle(
                Java19RecordPatterns.Point(int x1, int y1),
                Java19RecordPatterns.Point(int x2, int y2))) {
            assert x1 == 0 && y1 == 0;
            assert x2 == 10 && y2 == 10;
        }
    }

    @Test
    public void testRecordPatternInSwitch() {
        Java19RecordPatterns.Point p = new Java19RecordPatterns.Point(5, 5);
        
        String result = switch (p) {
            case Java19RecordPatterns.Point(0, 0) -> "Origin";
            case Java19RecordPatterns.Point(int x, int y) when x == y -> "Diagonal";
            case Java19RecordPatterns.Point(int x, int y) -> "Regular";
        };
        
        assert result.equals("Diagonal");
    }

    @Test
    public void testGenericRecordPattern() {
        Java19RecordPatterns.Box<String> box = new Java19RecordPatterns.Box<>("Test");
        
        String result = switch (box) {
            case Java19RecordPatterns.Box(String s) -> "String box";
            case Java19RecordPatterns.Box(null) -> "Empty";
            default -> "Other";
        };
        
        assert result.equals("String box");
    }

    @Test
    public void testRecordPatternWithUnderscore() {
        Java19RecordPatterns.Point p = new Java19RecordPatterns.Point(5, 10);
        
        // Using underscore to ignore y coordinate
        if (p instanceof Java19RecordPatterns.Point(int x, _)) {
            assert x == 5;
        }
    }

    @Test
    public void testRecordPatternWithGuard() {
        Java19RecordPatterns.Point p = new Java19RecordPatterns.Point(3, 3);
        
        String result = switch (p) {
            case Java19RecordPatterns.Point(int x, int y) when x == y ->
                "Equal coordinates";
            case Java19RecordPatterns.Point(int x, int y) ->
                "Different coordinates";
        };
        
        assert result.equals("Equal coordinates");
    }

    @Test
    public void testComplexRecordPattern() {
        Java19RecordPatterns.User user = 
            new Java19RecordPatterns.User("Alice", 30, "alice@example.com");
        
        String result = switch (user) {
            case Java19RecordPatterns.User(String name, int age, String email)
                    when age >= 18 ->
                "Adult: " + name;
            case Java19RecordPatterns.User(String name, int age, String email) ->
                "Minor: " + name;
        };
        
        assert result.equals("Adult: Alice");
    }

    // ============ Integration Tests ============
    
    @Test
    public void testCombinedPatterns() {
        Object[] data = {
            "Hello",
            42,
            new Java19RecordPatterns.Point(1, 1),
            null
        };
        
        for (Object obj : data) {
            String result = switch (obj) {
                case String s when s.length() > 0 -> "Non-empty string";
                case Integer i when i > 0 -> "Positive integer";
                case Java19RecordPatterns.Point(int x, int y) -> "Point at (" + x + "," + y + ")";
                case null -> "Null";
                default -> "Unknown";
            };
            assert result != null;
        }
    }

    @Test
    public void testPerformanceComparison() {
        Object obj = "Test String";
        
        // Pattern matching should be efficient
        long start = System.nanoTime();
        for (int i = 0; i < 1000000; i++) {
            if (obj instanceof String s) {
                s.length();
            }
        }
        long duration = System.nanoTime() - start;
        
        // Should complete in reasonable time
        assert duration < 5_000_000_000L; // 5 seconds
    }

    @Test
    public void testMultipleTypeHandling() {
        Object[] objects = {
            "string",
            123,
            45.67,
            true,
            java.util.List.of(1, 2, 3),
            null
        };
        
        int count = 0;
        for (Object obj : objects) {
            String type = switch (obj) {
                case String s -> "string";
                case Integer i -> "integer";
                case Double d -> "double";
                case Boolean b -> "boolean";
                case java.util.List l -> "list";
                case null -> "null";
                default -> "unknown";
            };
            count++;
        }
        
        assert count == 6;
    }

    @Test
    public void testDataValidation() {
        String email = "user@example.com";
        
        String validation = switch (email) {
            case String s when s.contains("@") && s.contains(".") ->
                "valid-email";
            case String s when !s.isBlank() ->
                "invalid-email";
            case String s ->
                "empty-email";
            default -> "error";
        };
        
        assert validation.equals("valid-email");
    }

    public static void main(String[] args) {
        System.out.println("Pattern Matching Tests");
        System.out.println("All test methods follow JUnit convention");
    }
}
