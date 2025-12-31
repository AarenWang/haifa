package org.wrj.modern;

import org.junit.Test;

/**
 * 现代 Java 特性测试用例
 */
public class ModernJavaFeaturesTest {

    @Test
    public void testVarKeyword() {
        var list = java.util.List.of(1, 2, 3);
        assert list.size() == 3;
        System.out.println("var keyword test passed");
    }

    @Test
    public void testRecord() {
        Person person = new Person("Alice", 30, "alice@example.com");
        System.out.println("Record: " + person);
        assert person.name().equals("Alice");
        assert person.age() == 30;
        System.out.println("Record test passed");
    }

    @Test
    public void testSealedClass() {
        Shape circle = new Circle(5.0);
        Shape rectangle = new Rectangle(4.0, 6.0);
        Shape triangle = new Triangle(3.0, 4.0);

        System.out.println("Circle area: " + circle.area());
        System.out.println("Rectangle area: " + rectangle.area());
        System.out.println("Triangle area: " + triangle.area());
        System.out.println("Sealed class test passed");
    }

    @Test
    public void testTextBlock() {
        String sql = """
                SELECT * FROM users
                WHERE age > 18
                AND status = 'active'
                """;
        assert sql.contains("SELECT");
        assert sql.contains("WHERE");
        System.out.println("Text block test passed");
    }

    @Test
    public void testPatternMatching() {
        Object obj = "Hello";
        String result;

        if (obj instanceof String s) {
            result = "String: " + s.toUpperCase();
        } else {
            result = "Not a string";
        }

        assert result.equals("String: HELLO");
        System.out.println("Pattern matching test passed");
    }
}
