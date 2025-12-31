# Java 模式匹配进化史（Java 11 - 21）

## 概述

模式匹配是 Java 演进中最重要的特性之一。从 Java 16 开始，模式匹配逐步成为核心语言特性，大大简化了代码的复杂性。

---

## 历史演进

### Java 11-15 之前：无原生模式匹配

```java
// 传统方式：冗长的类型检查和转换
Object obj = "Hello";
if (obj instanceof String) {
    String str = (String) obj;  // 需要显式转换
    System.out.println(str.toUpperCase());
}
```

**问题**：
- 重复的类型检查和转换
- 代码冗长且容易出错
- 不符合函数式编程风格

---

## Java 16: instanceof 模式匹配（第一阶段）

### 基本的 instanceof 模式

**JEP 394**

#### 语法

```java
if (obj instanceof String s) {
    // 在此作用域内，s 已被安全转换为 String
    System.out.println(s.toUpperCase());
}
```

#### 特点
- 消除了显式类型转换
- 变量 `s` 仅在该分支作用域内有效
- 自动进行类型检查和转换

#### 完整示例

```java
public class Java16PatternMatching {
    public static void analyze(Object obj) {
        if (obj instanceof String s) {
            System.out.println("字符串长度: " + s.length());
        } else if (obj instanceof Integer i) {
            System.out.println("整数值: " + i);
        } else if (obj instanceof Double d) {
            System.out.println("浮点值: " + d);
        } else if (obj instanceof Boolean b) {
            System.out.println("布尔值: " + b);
        } else {
            System.out.println("未知类型");
        }
    }
}
```

#### 模式的细化

```java
// 结合逻辑运算符
if (obj instanceof String s && s.length() > 5) {
    System.out.println("长字符串: " + s);
}

if (obj instanceof String s && !s.isBlank()) {
    System.out.println("非空字符串: " + s);
}
```

---

## Java 17: switch 表达式 + 模式匹配

### Switch 表达式增强

从 Java 12 开始，switch 表达式成为预览特性，Java 17 中最终确定。

#### 传统 Switch 语句的问题

```java
// 旧风格：容易遗漏 break
String type;
switch (obj) {
    case String s:
        type = "String";
        break;  // 容易忘记
    case Integer i:
        type = "Integer";
        break;
    default:
        type = "Unknown";
}
```

#### 现代 Switch 表达式

```java
String type = switch (obj) {
    case String s -> "String";
    case Integer i -> "Integer";
    case Double d -> "Double";
    case Boolean b -> "Boolean";
    default -> "Unknown";
};
```

#### Switch 中的模式

```java
public class Java17SwitchPatterns {
    
    public static int process(Object obj) {
        return switch (obj) {
            case null -> -1;  // null 模式
            case String s -> s.length();
            case Integer i -> i;
            case Double d -> (int) d.doubleValue();
            default -> 0;
        };
    }

    public static String describe(Object obj) {
        return switch (obj) {
            case String s when s.length() > 10 -> "长字符串";
            case String s when s.isBlank() -> "空字符串";
            case String s -> "短字符串";
            case Integer i when i < 0 -> "负整数";
            case Integer i when i == 0 -> "零";
            case Integer i -> "正整数";
            case null -> "null";
            default -> "其他类型";
        };
    }
}
```

**关键特性**：
- `->` 语法，不需要 break
- 保证完整性检查（编译器验证所有情况被处理）
- 支持 `when` 守卫条件
- 支持 null 模式

---

## Java 19-20: Record 模式匹配（预览）

### Record 的基本概念

```java
// Record 定义（Java 14+）
public record Point(int x, int y) {}
public record Color(int red, int green, int blue) {}
```

### Record 模式

**JEP 405 (Java 19)**、**JEP 420 (Java 20)**

#### 基本用法

```java
// 分解 Record
record Point(int x, int y) {}

Point p = new Point(10, 20);

if (p instanceof Point(int x, int y)) {
    System.out.println("X: " + x + ", Y: " + y);
}
```

#### 嵌套 Record 模式

```java
record Rectangle(Point topLeft, Point bottomRight) {}

Rectangle rect = new Rectangle(
    new Point(0, 0),
    new Point(10, 10)
);

if (rect instanceof Rectangle(Point(int x1, int y1), Point(int x2, int y2))) {
    System.out.println("宽: " + (x2 - x1) + ", 高: " + (y2 - y1));
}
```

#### Switch 中的 Record 模式

```java
public sealed interface Shape permits Circle, Rectangle {
}

public record Circle(double radius) implements Shape {}
public record Rectangle(double width, double height) implements Shape {}

public static double getArea(Shape shape) {
    return switch (shape) {
        case Circle(double r) -> Math.PI * r * r;
        case Rectangle(double w, double h) -> w * h;
    };
}
```

#### 配合 Sealed Classes

```java
public sealed interface Vehicle permits Car, Bike, Truck {}

public record Car(String brand, int seats) implements Vehicle {}
public record Bike(String brand, boolean hasCart) implements Vehicle {}
public record Truck(String brand, double capacity) implements Vehicle {}

public static void describeVehicle(Vehicle vehicle) {
    switch (vehicle) {
        case Car(String brand, int seats) -> 
            System.out.println("汽车: " + brand + ", 座位数: " + seats);
        case Bike(String brand, boolean cart) ->
            System.out.println("自行车: " + brand + ", 有货架: " + cart);
        case Truck(String brand, double cap) ->
            System.out.println("卡车: " + brand + ", 容量: " + cap);
    }
}
```

---

## Java 21: Record 模式最终版 + 增强特性

### Record 模式成为标准

**JEP 440** - Record Patterns 正式进入标准

#### 完整的 Record 模式示例

```java
public record Box<T>(T item) {}
public record Wrapper(Box<?> box) {}

public static String identify(Object obj) {
    return switch (obj) {
        // 泛型 Record 模式
        case Box(String s) -> "String box: " + s;
        case Box(Integer i) -> "Integer box: " + i;
        case Box(null) -> "Empty box";
        
        // 嵌套 Record 模式
        case Wrapper(Box(String s)) -> "Wrapped string: " + s;
        case Wrapper(Box(null)) -> "Wrapped null";
        
        default -> "Unknown";
    };
}
```

### 数组模式（Java 21）

虽然还在预览，但给出示例：

```java
// 数组模式示例（概念）
if (arr instanceof String[] {String first, ...}) {
    System.out.println("数组首元素: " + first);
}
```

### 类型模式的完整层级

Java 21 中的模式匹配支持以下层级：

```
Pattern
├── Type Pattern (类型模式)
│   ├── Instanceof Pattern (instanceof 检查)
│   ├── Record Pattern (Record 分解)
│   └── Guard Pattern (带守卫)
├── Constant Pattern (常量模式)
├── Var Pattern (变量绑定)
└── Null Pattern (null 检查)
```

---

## 完整对比示例

### 场景：处理多种数据类型的 API 响应

#### Java 11 风格（传统方式）

```java
public String processResponse(Object response) {
    if (response instanceof String) {
        String str = (String) response;
        if (str.isEmpty()) return "空字符串";
        return "字符串: " + str;
    }
    
    if (response instanceof Map) {
        Map map = (Map) response;
        if (map.containsKey("error")) {
            return "错误: " + map.get("error");
        }
        return "数据: " + map.get("data");
    }
    
    if (response instanceof List) {
        List list = (List) response;
        return "列表大小: " + list.size();
    }
    
    return "未知类型";
}
```

#### Java 16 风格（instanceof 模式）

```java
public String processResponse(Object response) {
    if (response instanceof String s) {
        return s.isEmpty() ? "空字符串" : "字符串: " + s;
    }
    
    if (response instanceof Map map && map.containsKey("error")) {
        return "错误: " + map.get("error");
    }
    
    if (response instanceof List list) {
        return "列表大小: " + list.size();
    }
    
    return "未知类型";
}
```

#### Java 17 风格（switch 表达式）

```java
public String processResponse(Object response) {
    return switch (response) {
        case String s when s.isEmpty() -> "空字符串";
        case String s -> "字符串: " + s;
        case Map map when map.containsKey("error") -> "错误: " + map.get("error");
        case List list -> "列表大小: " + list.size();
        case null -> "空响应";
        default -> "未知类型";
    };
}
```

#### Java 21 风格（Record + 模式匹配）

```java
// 定义 Record
record StringResponse(String value) {}
record ErrorResponse(String code, String message) {}
record DataResponse(Map<String, Object> data) {}
record ListResponse(List<?> items) {}

// 处理
public String processResponse(Object response) {
    return switch (response) {
        case StringResponse(String s) when s.isEmpty() -> "空字符串";
        case StringResponse(String s) -> "字符串: " + s;
        case ErrorResponse(String code, String msg) -> "错误 " + code + ": " + msg;
        case DataResponse(Map data) -> "数据项: " + data.size();
        case ListResponse(List items) -> "列表大小: " + items.size();
        case null -> "空响应";
        default -> "未知类型";
    };
}
```

---

## 最佳实践

### 1. 优先使用模式匹配

```java
// ❌ 不推荐
if (obj instanceof String) {
    String s = (String) obj;
    System.out.println(s);
}

// ✅ 推荐
if (obj instanceof String s) {
    System.out.println(s);
}
```

### 2. 充分利用 switch 表达式

```java
// ❌ 不推荐
String result;
if (obj instanceof String s) {
    result = s.toUpperCase();
} else if (obj instanceof Integer i) {
    result = String.valueOf(i);
} else {
    result = "unknown";
}

// ✅ 推荐
String result = switch (obj) {
    case String s -> s.toUpperCase();
    case Integer i -> String.valueOf(i);
    default -> "unknown";
};
```

### 3. 利用 sealed classes 和模式匹配

```java
// ✅ 推荐：sealed 类使 switch 完整性检查更有效
public sealed interface Command permits QueryCommand, UpdateCommand, DeleteCommand {}
public record QueryCommand(String query) implements Command {}
public record UpdateCommand(String table, String data) implements Command {}
public record DeleteCommand(String id) implements Command {}

public static void execute(Command cmd) {
    switch (cmd) {
        case QueryCommand(String q) -> query(q);
        case UpdateCommand(String t, String d) -> update(t, d);
        case DeleteCommand(String id) -> delete(id);
        // 编译器确保所有情况已处理
    };
}
```

### 4. 使用守卫条件增加可读性

```java
// ✅ 好的实践
String status = switch (obj) {
    case Integer i when i < 0 -> "负数";
    case Integer i when i == 0 -> "零";
    case Integer i -> "正数";
    default -> "非整数";
};
```

### 5. 嵌套模式用于复杂数据结构

```java
// ✅ 推荐
record Response<T>(Status status, T data) {}
enum Status { SUCCESS, ERROR }

String info = switch (response) {
    case Response(Status.SUCCESS, String data) -> "成功: " + data;
    case Response(Status.SUCCESS, Integer num) -> "数字: " + num;
    case Response(Status.ERROR, var data) -> "失败: " + data;
    default -> "未知";
};
```

---

## 编译器和版本要求

| 版本 | 特性 | 状态 | JEP |
|------|------|------|-----|
| Java 16 | instanceof 模式 | 最终版 | 394 |
| Java 17 | switch 模式 | 最终版 | 406 |
| Java 19 | Record 模式 | 预览 | 405 |
| Java 20 | Record 模式 | 第二预览 | 420 |
| Java 21 | Record 模式 | 最终版 | 440 |

---

## 运行和测试

### 编译选项

```bash
# Java 19-20（预览特性需要明确开启）
javac --enable-preview PatternMatchingDemo.java

# Java 21+（Record 模式已成为标准）
javac PatternMatchingDemo.java
```

### 运行选项

```bash
# Java 19-20
java --enable-preview PatternMatchingDemo

# Java 21+
java PatternMatchingDemo
```

---

## 相关资源

- [JEP 394: Pattern Matching for instanceof](https://openjdk.org/jeps/394)
- [JEP 406: Pattern Matching for switch (Preview)](https://openjdk.org/jeps/406)
- [JEP 405: Record Patterns (Preview)](https://openjdk.org/jeps/405)
- [JEP 440: Record Patterns](https://openjdk.org/jeps/440)
- [Java Language Specification](https://docs.oracle.com/javase/specs/)
