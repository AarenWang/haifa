# Java Record 教程（概要，适用于 Java 16+，推荐 Java 17/21）

本教程介绍 Java `record` 的用途、语法以及常见用法，并配套多个可运行的示例代码，帮助你理解记录类型如何简化不变数据载体的定义。

什么是 `record`？

- `record` 是一种不可变的数据载体，自动生成 `equals/hashCode/toString`、访问器方法和构造器的样板代码。适用于 DTO、键、值对象等场景。
- 引入版本：Java 16（正式），但在早期是预览特性。建议使用 Java 17 或更新版本以获得更稳定的体验。

语法示例

```java
public record PersonRecord(String name, int age) implements java.io.Serializable {}
```

要点说明

- 组件（components）：记录头中的字段，例如 `name`、`age`，会自动生成不可变的访问器 `name()`、`age()`。
- 典型生成：编译器会为记录生成一个 `public` 的构造器、`equals`、`hashCode` 以及 `toString`。
- 自定义构造器：可以定义“紧凑构造器”（compact constructor）用于校验或调整参数；也可以定义完整的自定义构造器。
- 方法与接口：记录可以定义实例方法、静态方法、实现接口，但不能显式继承类（隐式继承 `java.lang.Record`）。
- 序列化：记录可以实现 `Serializable`，但要注意序列化兼容性与自定义序列化规则。
- 与模式匹配配合：记录与模式匹配（record patterns / instanceof pattern）配合使用，可以更方便地解构记录（在较新 Java 版本中可用）。

示例列表（位于 `src/main/java/org/wrj/haifa/modern/record/`）

- `PersonRecord.java`：一个简单的 `record` 定义（实现 `Serializable`）。
- `ValidationRecordExample.java`：使用紧凑构造器做入参校验的示例。
- `RecordMethodsExample.java`：在 `record` 中添加方法与计算派生值。
- `RecordPatternExample.java`：使用 `instanceof` 模式匹配解构 `record` 并演示匹配使用。
- `RecordSerializationExample.java`：将记录序列化为字节数组并反序列化回对象的示例。

编译与运行

推荐 JDK：Java 17 或 Java 21。若使用早期支持的 JDK，请确保启用了必要的预览或 incubator 标志。

编译：

```bash
javac -d out $(find src/main/java -name "*.java")
```

运行示例：

```bash
java -cp out org.wrj.haifa.modern.record.ValidationRecordExample
```

最佳实践

- 用 `record` 表示逻辑上不可变、只包含数据且行为较少的类型。
- 如果需要可变字段或复杂继承层次，请使用普通类。
- 对于需要自定义序列化的记录，考虑实现 `readObject` / `writeObject` 或使用外部转换机制。

下一步

- 我可以将这些示例整合进 `pom.xml`（添加 `exec` 任务），或为每个示例添加单元测试。告诉我你更希望我做哪项。
