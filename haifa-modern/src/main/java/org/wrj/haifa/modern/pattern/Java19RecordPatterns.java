package org.wrj.modern.pattern;

import java.util.List;

/**
 * Java 19-21: Record 模式匹配
 *
 * JEP 405 (Java 19), JEP 420 (Java 20), JEP 440 (Java 21)
 * 
 * Record 模式允许在 instanceof 和 switch 中直接分解 Record 字段
 * 这是函数式编程和模式匹配的完美结合
 */
public class Java19RecordPatterns {

    // ============ 定义 Record 类 ============
    
    public record Point(int x, int y) {
        public boolean isOrigin() {
            return x == 0 && y == 0;
        }
    }

    public record Rectangle(Point topLeft, Point bottomRight) {
    }

    public record Circle(Point center, int radius) {
    }

    public record Box<T>(T item) {
    }

    // API 响应的 Record 定义
    public record ApiResponse<T>(int code, String message, T data) {
    }

    public record User(String name, int age, String email) {
    }

    public record Product(String id, String name, double price) {
    }

    // ============ Sealed Interface 与 Record ============
    
    public sealed interface Animal permits Dog, Cat, Bird {}
    
    public record Dog(String name, String breed) implements Animal {}
    public record Cat(String name, int lives) implements Animal {}
    public record Bird(String name, int wingSpan) implements Animal {}

    public sealed interface Shape permits CircleShape, RectangleShape {}
    
    public record CircleShape(double radius) implements Shape {}
    public record RectangleShape(double width, double height) implements Shape {}

    // ============ 基本 Record 模式 ============
    
    /**
     * 基本的 Record 分解
     */
    public static void basicRecordPattern() {
        System.out.println("=== Basic Record Pattern ===");
        
        Point p = new Point(10, 20);
        
        // 在 instanceof 中分解 Record
        if (p instanceof Point(int x, int y)) {
            System.out.println("Point coordinates: x=" + x + ", y=" + y);
        }
        
        // 使用 var 简化
        if (p instanceof Point(var x, var y)) {
            System.out.println("Using var: x=" + x + ", y=" + y);
        }
        System.out.println();
    }

    /**
     * 嵌套 Record 模式
     */
    public static void nestedRecordPattern() {
        System.out.println("=== Nested Record Pattern ===");
        
        Rectangle rect = new Rectangle(
            new Point(0, 0),
            new Point(10, 10)
        );
        
        // 一次性分解嵌套的 Record
        if (rect instanceof Rectangle(Point(int x1, int y1), Point(int x2, int y2))) {
            int width = x2 - x1;
            int height = y2 - y1;
            System.out.println("Rectangle dimensions: " + width + " x " + height);
        }
        System.out.println();
    }

    /**
     * Switch 中的 Record 模式
     */
    public static String analyzePoint(Object obj) {
        System.out.println("=== Record Pattern in Switch ===");
        
        return switch (obj) {
            case Point(int x, int y) when x == 0 && y == 0 ->
                "Origin point";
            case Point(int x, int y) when x == y ->
                "Diagonal point: (" + x + ", " + y + ")";
            case Point(int x, int y) when x > y ->
                "X-dominant: (" + x + ", " + y + ")";
            case Point(int x, int y) ->
                "Regular point: (" + x + ", " + y + ")";
            default ->
                "Not a point";
        };
    }

    /**
     * 处理泛型 Record
     */
    public static <T> String analyzeBox(Object obj) {
        System.out.println("=== Generic Record Pattern ===");
        
        return switch (obj) {
            case Box(String s) -> "String box: " + s;
            case Box(Integer i) -> "Integer box: " + i;
            case Box(Point(int x, int y)) -> "Point box: (" + x + ", " + y + ")";
            case Box(null) -> "Empty box";
            case Box(var item) -> "Box with: " + item.getClass().getSimpleName();
            default -> "Not a box";
        };
    }

    /**
     * 与 Sealed Classes 结合
     */
    public static String describeAnimal(Animal animal) {
        System.out.println("=== Sealed Class with Record Pattern ===");
        
        return switch (animal) {
            case Dog(String name, String breed) ->
                "Dog named " + name + " of breed " + breed;
            case Cat(String name, int lives) ->
                "Cat named " + name + " with " + lives + " lives";
            case Bird(String name, int wingSpan) ->
                "Bird named " + name + " with wingspan " + wingSpan;
        };
    }

    /**
     * API 响应处理
     */
    public static String handleApiResponse(ApiResponse<?> response) {
        System.out.println("=== API Response Handling ===");
        
        return switch (response) {
            case ApiResponse(200, String msg, User(String name, int age, String email)) ->
                "User: " + name + " (age: " + age + ", email: " + email + ")";
            case ApiResponse(200, String msg, Product(String id, String pname, double price)) ->
                "Product: " + pname + " (id: " + id + ", price: $" + price + ")";
            case ApiResponse(200, String msg, List list) ->
                "Got " + list.size() + " items";
            case ApiResponse(200, _, _) ->
                "Success: " + response.code();
            case ApiResponse(code, String msg, _) when code >= 400 ->
                "Error " + code + ": " + msg;
            case ApiResponse(code, _, _) ->
                "Response code: " + code;
        };
    }

    /**
     * 深层嵌套的 Record 模式
     */
    public static void deepNestedPattern() {
        System.out.println("=== Deep Nested Pattern ===");
        
        // 创建深层嵌套的数据
        Box<Rectangle> boxedRect = new Box<>(
            new Rectangle(
                new Point(0, 0),
                new Point(5, 5)
            )
        );
        
        // 一次性分解所有层级
        String result = switch (boxedRect) {
            case Box(Rectangle(Point(int x1, int y1), Point(int x2, int y2))) ->
                "Boxed rectangle from (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")";
            default -> "Unknown";
        };
        
        System.out.println(result);
        System.out.println();
    }

    /**
     * 使用下划线 (_) 忽略不需要的字段
     */
    public static void patternWithUnderscore() {
        System.out.println("=== Pattern with Underscore ===");
        
        Point p = new Point(3, 4);
        User u = new User("Alice", 30, "alice@example.com");
        
        // 只关心 x 坐标，忽略 y
        if (p instanceof Point(int x, _)) {
            System.out.println("X coordinate: " + x);
        }
        
        // 只关心用户名，忽略其他字段
        if (u instanceof User(String name, _, _)) {
            System.out.println("User name: " + name);
        }
        System.out.println();
    }

    /**
     * 与守卫条件结合
     */
    public static String analyzeRectangle(Rectangle rect) {
        System.out.println("=== Record Pattern with Guard ===");
        
        return switch (rect) {
            case Rectangle(Point(int x1, int y1), Point(int x2, int y2)) 
                    when x2 - x1 == y2 - y1 ->
                "Square with side: " + (x2 - x1);
            case Rectangle(Point(int x1, int y1), Point(int x2, int y2))
                    when (x2 - x1) > (y2 - y1) ->
                "Wide rectangle";
            case Rectangle(Point(int x1, int y1), Point(int x2, int y2)) ->
                "Tall rectangle";
        };
    }

    /**
     * 实际应用：数据映射和转换
     */
    public static class DataProcessor {
        
        public static String processUser(Object data) {
            return switch (data) {
                case User(String name, int age, String email) when age >= 18 ->
                    "Adult user: " + name;
                case User(String name, int age, String email) when age < 18 ->
                    "Minor user: " + name;
                default -> "Invalid user";
            };
        }
        
        public static double calculateArea(Shape shape) {
            return switch (shape) {
                case CircleShape(double r) -> Math.PI * r * r;
                case RectangleShape(double w, double h) -> w * h;
            };
        }
    }

    /**
     * 复杂的列表处理
     */
    public static void processPointList() {
        System.out.println("=== Process Point List ===");
        
        List<Point> points = List.of(
            new Point(0, 0),
            new Point(5, 5),
            new Point(10, 0)
        );
        
        for (Point p : points) {
            String description = switch (p) {
                case Point(0, 0) -> "Origin";
                case Point(var x, var y) when x == y -> "Diagonal point (" + x + ", " + y + ")";
                case Point(var x, var y) -> "Point (" + x + ", " + y + ")";
            };
            System.out.println(description);
        }
        System.out.println();
    }

    public static void main(String[] args) {
        basicRecordPattern();
        nestedRecordPattern();
        
        System.out.println(analyzePoint(new Point(0, 0)));
        System.out.println(analyzePoint(new Point(5, 5)));
        System.out.println(analyzePoint(new Point(10, 5)));
        System.out.println();
        
        System.out.println(analyzeBox(new Box<>("Hello")));
        System.out.println(analyzeBox(new Box<>(42)));
        System.out.println(analyzeBox(new Box<>(new Point(3, 4))));
        System.out.println();
        
        System.out.println(describeAnimal(new Dog("Buddy", "Golden Retriever")));
        System.out.println(describeAnimal(new Cat("Whiskers", 9)));
        System.out.println(describeAnimal(new Bird("Tweety", 30)));
        System.out.println();
        
        System.out.println(handleApiResponse(
            new ApiResponse<>(200, "OK", new User("John", 25, "john@example.com"))
        ));
        System.out.println();
        
        deepNestedPattern();
        patternWithUnderscore();
        
        System.out.println(analyzeRectangle(new Rectangle(new Point(0, 0), new Point(5, 5))));
        System.out.println(analyzeRectangle(new Rectangle(new Point(0, 0), new Point(10, 5))));
        System.out.println();
        
        System.out.println(DataProcessor.processUser(new User("Alice", 25, "alice@example.com")));
        System.out.println(DataProcessor.processUser(new User("Bob", 16, "bob@example.com")));
        System.out.println();
        
        System.out.println("Circle area: " + DataProcessor.calculateArea(new CircleShape(5)));
        System.out.println("Rectangle area: " + DataProcessor.calculateArea(new RectangleShape(4, 6)));
        System.out.println();
        
        processPointList();
    }
}
