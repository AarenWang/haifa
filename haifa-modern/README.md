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
