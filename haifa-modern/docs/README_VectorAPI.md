# Java Vector API 学习指南（截至 Java 21）

本指南说明如何在 `haifa-modern` 模块里使用并学习 Java Vector API（jdk.incubator.vector），并提供若干简单示例（向量加法、掩码操作、归约求和、带权和），演示如何将标量循环替换为向量化操作以获取更好性能。

兼容性与准备

- 推荐 JDK：JDK 17 或 JDK 21（Vector API 在多个 Java 版本中处于 incubator/preview 状态，请根据你使用的 JDK 添加相应模块／标志）。
- 典型编译/运行时需要额外模块标志：`--add-modules jdk.incubator.vector`。某些 JDK 版本可能还需要 `--enable-preview`，请参考你的 JDK 文档。

示例说明

示例代码位于：`src/main/java/org/wrj/haifa/modern/vectorapi/`，每个示例类都包含 `main` 方法，可独立运行。

- `VectorAddExample`：基础的向量加法（数组加法并回写结果）。
- `VectorMaskExample`：展示如何使用掩码处理尾部不对齐元素（length 不是 lane 长度倍数时）。
- `VectorReduceExample`：使用向量做归约（求和）。
- `VectorWeightedSumExample`：带权和示例，结合向量乘法与累加。

快速运行（bash）

编译：

```bash
javac --add-modules jdk.incubator.vector -d out $(find src/main/java -name "*.java")
```

运行（以 `VectorAddExample` 为例）：

```bash
java --add-modules jdk.incubator.vector -cp out org.wrj.haifa.modern.vectorapi.VectorAddExample
```

（如果使用 Maven 构建并希望通过 `exec:java` 运行，可能需要在 `pom.xml` 中为 `exec` 插件添加 `--add-modules=jdk.incubator.vector` 到 `argLine` 或 `jvmArgs`。）

测验与验证

每个示例内都包含基本的正确性校验（与纯标量实现结果比较）。建议在目标硬件上对比向量化与标量实现的性能差异，注意热启动和 JIT 编译的影响。

下一步建议

- 如果需要，我可以：
  - 为每个示例添加单元测试或 JMH 基准。  
  - 把这些示例集成到模块的 `pom.xml` 中，添加 `exec` 任务以便用 `mvn` 直接运行。
