# haifa-new-akka 教学文档

## 1. 项目所涉及的核心知识

- **Akka Typed ActorSystem 与行为模型**：项目通过 `ActorSystem.create` 启动强类型演员系统，并使用 `Behavior` 描述状态机式的消息处理流程；`Greeter`、`GreeterBot`、`GreeterMain` 三个类构成完整的对话闭环。
- **消息协议与一问一答模式**：`Greeter` 与 `GreeterBot` 之间采用“请求-响应”协议，通过消息中携带的 `replyTo` 引用实现解耦回调，是 Akka 中常见的对话模式。
- **Actor 生命周期与监督**：IoT 示例演示了设备注册、终止监控与 `PostStop` 钩子，帮助理解在分布式系统中管理 Actor 的生命周期。
- **Akka TestKit Typed 测试模式**：测试模块使用 `TestKitJunitResource`、`TestProbe` 构建端到端行为测试，展示如何在 Java 中编写可重复的并发测试。
- **依赖与日志配置**：`pom.xml` 声明 `akka-actor-typed`、`akka-actor-testkit-typed` 与 Logback/SLF4J 依赖，为 Actor 调试提供基础设施。

## 2. 项目包含的 Demo 功能

1. **Typed Greeter 快速入门**：`org.wrj.haifa.akka.newstyle` 包复刻官方 Quickstart，演示如何构建 `Greeter`、`GreeterBot` 与 `GreeterMain` 三个 Actor 协作完成问候循环，并在 `AkkaQuickstart` 中启动系统。
2. **经典 API Quickstart（对比学习）**：`com.lightbend.akka.sample.quickstart` 包保留了传统 `AbstractActor` 样例，帮助对比 Typed 与 Classic API 在 Props、消息定义上的差异。
3. **IoT 设备管理案例**：`com.lightbend.akka.sample.iot` 提供设备注册、温度采集、分组查询等复杂交互示例，并配套 JUnit 测试验证 Actor 拓扑、查询聚合与容错逻辑。
4. **Actor 层级探索**：`com.lightbend.akka.sample.ActorHierarchySample.ActorHierarchyExperiments` 展示如何在层级间创建/停止子 Actor，强调父子间消息传播与监督概念。

## 3. 在项目中集成 Akka 的步骤

1. **引入依赖与版本管理**：在模块 `pom.xml` 中配置 `akka.version` 属性，并添加 `akka-actor-typed`、`akka-actor-testkit-typed`、SLF4J/Logback 等依赖，确保运行与测试环境一致。
2. **定义领域消息协议**：为每类 Actor 声明封闭的消息类型（类或接口），并在消息中携带上下文信息（如 `replyTo`、`requestId`），便于实现强类型通信。
3. **实现行为与状态**：继承 `AbstractBehavior`（Typed）或 `AbstractActor`（Classic），在 `createReceive` 中通过 `newReceiveBuilder()`/`receiveBuilder()` 定义消息处理逻辑，并在需要时通过私有字段维护状态（例如温度读数、计数器）。
4. **组装 Actor 拓扑**：通过 `ActorContext.spawn` 创建子 Actor，结合 `watchWith`、`spawnAnonymous` 管理生命周期和临时查询 Actor，以支撑复杂业务流程。
5. **启动 ActorSystem 并发送消息**：在 `main` 方法中创建 `ActorSystem`，持有顶级 Guardian 行为（如 `GreeterMain` 或 `IotSupervisor`），并使用 `tell` 触发对话；退出前调用 `terminate` 安全关闭系统。
6. **编写行为测试**：利用 `TestKitJunitResource` 和 `TestProbe` 编写交互式测试，模拟 Actor 之间的消息往来，确保在并发环境下逻辑正确无死锁。

> 建议按照上述步骤扩展业务 Actor：先定义消息协议，再实现行为与测试，最后通过顶层 `ActorSystem` 集成进应用的启动流程。
