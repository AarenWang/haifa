# haifa-modern — 类型推导学习与示例

本模块收集了与 Java 类型推导（type inference）相关的学习资料与可运行示例，覆盖从 Java 11 到 Java 21 常用或现代化的类型推导用法（示例代码基于能够运行这些特性的较新 JDK）。

快速目录
- **概览**：本文件对关键特性做简要说明。
- **示例源码**：位于 `src/main/java/org/wrj/haifa/modern/typeinference/` 下的若干演示程序。
- **运行**：如何用 `javac`/`java` 或 Maven 运行示例。

概览（要点）

- `var`（局部变量类型推导）: 由 Java 10 引入，Java 11 及更高版本均支持。用于局部变量、增强 for、try-with-resources 等场景。
- `var` 在 lambda 参数中的使用：可以在 lambda 参数上使用 `var`（带来统一的声明语法，便于使用注解），需要较新的 JDK 支持。
- 模式匹配（Pattern Matching） for `instanceof`：可以在 `instanceof` 检查时直接绑定变量，避免显式强转（从较新 Java 版本开始逐步标准化）。
- Switch 表达式：`switch` 可作为表达式返回值，写法更简洁，配合类型推导可写出更清晰代码。
- `record`：轻量不可变数据载体（自带构造器/访问器），与类型推导配合可写出非常简洁的示例代码。
- 泛型推断改进：方法调用、构造器钻石操作符和集合工厂方法（`List.of` / `Map.of`）在类型推断上更智能，常用时会自动推断泛型参数类型。

示例列表与文件

- `VarExample` — 演示 `var` 在局部变量、循环、集合等场景下的使用。  
- `LambdaVarExample` — 演示在 lambda 参数上使用 `var` 的写法。  
- `PatternMatchingInstanceofExample` — 演示 `instanceof` 模式匹配的用法（绑定变量）。
- `SwitchExpressionExample` — 演示 `switch` 表达式与类型推导配合使用。  
- `Point`（record）与 `RecordExample` — 演示 `record` 与 `var` 结合使用。  
- `GenericInferenceExample` — 演示泛型方法/构造器以及类型推断示例。

运行说明

- 推荐 JDK：为了覆盖所有示例，建议使用 JDK 17 或 JDK 21（若使用较老 JDK，某些示例可能不可用）。
- 使用 javac + java（示例）：

```bash
javac -d out $(find src/main/java -name "*.java")
java -cp out org.wrj.haifa.modern.typeinference.VarExample
```

- 使用 Maven（若模块配置完整）：

```bash
mvn -pl haifa-modern -am compile
mvn -pl haifa-modern -am exec:java -Dexec.mainClass="org.wrj.haifa.modern.typeinference.VarExample"
```

更多说明

示例代码直接放在 `src/main/java/org/wrj/haifa/modern/typeinference/`，每个类均含 `main` 方法，可单独运行以观察输出。文档和示例旨在教学用途，重点展示“如何使用”和“为什么这样更清晰”。

如果需要，我可以：
- 为每个示例添加单元测试或构建脚本。  
- 将 README 中的版本注记调整为精确的 JEP/版本引用（需要我去查阅 JEP 列表）。
# Haifa Modern Java Features

此模块用于学习和展示 Java 11 以后的新增功能。

## 主要特性

### Java 10
- **var 局部变量类型推断** - 减少冗长的类型声明

### Java 11
- String 新方法：`isBlank()`, `strip()`, `lines()`
- 文件 I/O 增强
- HTTP Client API (孵化)

### Java 12-13
- **Switch 表达式** (预览)
- **文本块** (Text Blocks) - 多行字符串

### Java 14-15
- **Records** - 不可变数据对象
- **instanceof 模式匹配** (预览)
- **Sealed Classes** (预览)
- 改进的 NullPointerException 诊断

### Java 16+
- **Records** (最终版)
- **instanceof 模式匹配** (最终版)
- **Sealed Classes** (最终版)
- **日期时间格式化** 增强

### Java 17
- 密封类成为标准特性
- 模式匹配改进

### Java 18-20
- 虚拟线程 (Virtual Threads) 预览
- 模式匹配扩展

### Java 21
- **虚拟线程** (最终版) - 轻量级并发
- **记录模式** (Record Patterns)
- **switch 模式匹配** (最终版)
- **序列化的过滤** 改进

## 模块结构

```
haifa-modern/
├── src/main/java/org/wrj/modern/
│   ├── ModernJavaFeatures.java       # 特性演示
│   ├── Person.java                   # Record 示例
│   └── Shape.java                    # Sealed Class 示例
└── src/test/java/org/wrj/modern/
    └── ModernJavaFeaturesTest.java   # 测试用例
```

## 运行示例

```bash
# 编译模块
mvn -pl haifa-modern clean compile

# 运行测试
mvn -pl haifa-modern test

# 运行主程序
mvn -pl haifa-modern exec:java -Dexec.mainClass="org.wrj.modern.ModernJavaFeatures"
```

## 学习重点

1. **类型推断** - `var` 关键字的使用场景和限制
2. **不可变数据** - Records 如何简化 DTO/POJO 创建
3. **类型安全** - Sealed Classes 带来的设计优势
4. **文本处理** - 文本块简化多行字符串
5. **模式匹配** - switch 表达式和 instanceof 模式的演变
6. **并发** - 虚拟线程改变并发编程范式

## 参考资源

- [Java Language Updates](https://docs.oracle.com/en/java/javase/)
- [JEP (Java Enhancement Proposals)](https://openjdk.java.net/jeps/)
- [OpenJDK 新特性](https://openjdk.java.net/)
