# Haifa 项目概览

Haifa 是一个多模块的 Maven 工作空间，用来系统梳理 Java 生态：从语言基础、并发编程到各类框架、中间件、分布式系统以及 AI 集成。每个 `haifa-*` 模块都是一个独立的实验或学习笔记，记录在日常开发中复盘与探索的主题。

## 仓库概况
- 父级构建：根目录下的 `pom.xml` 聚合 40+ 模块，统一使用 Java 25。
- 命名约定：模块命名为 `haifa-<topic>`（如 `haifa-concurrent`、`haifa-springframework`）。
- 代码布局：Java 源码位于 `org.wrj` 包下，对应资源文件位于 `src/main/resources`。
- 许可证：MIT License，扩展时请保持版权及许可证声明。

## 模块分层
- 语言与基础：`haifa-base`、`haifa-java-library`、`haifa-my-library`、`haifa-jvm-internal`、`haifa-legacy`、`haifa-block`、`haifa-vs`
- 并发与协同：`haifa-concurrent`、`haifa-scheduler`、`haifa-coordinate`、`haifa-reactor`
- 框架深度学习：`haifa-springframework`、`haifa-springboot`、`haifa-springcloud-nacos`、`haifa-springsecurity`、`haifa-dubbo`、`haifa-java-ee`、`haifa-web-container`、`haifa-template-engine`、`haifa-markup-language`
- 集成与中间件：`haifa-mq`（Kafka/RabbitMQ/RocketMQ 案例）、`haifa-cache`、`haifa-httpclient`、`haifa-aws`、`haifa-websocket`、`haifa-grpc`、`haifa-netty`
- 数据、搜索与区块链：`haifa-mongodb`、`haifa-search`、`haifa-json`、`haifa-nlp`、`haifa-web3j`
- 大数据与流式计算：`haifa-flink`、`haifa-flink13`
- 设计模式、测试与算法：`haifa-design-pattern`、`haifa-junit5`、`haifa-leetcode`
- AI 实验：`haifa-ai-alibaba`、`haifa-ai-spring`
- 工具与 CI：`haifa-ci-cd`、`haifa-empty`

> 提示：部分模块暂未包含 README，探索时可先阅读包名、测试或源码注释。

## 技术要点
- 统一的 Java 25 开发环境与 Maven 3.x 构建流程。
- 覆盖 Spring 全家桶、Netty、Dubbo、Reactor、Akka、Flink 等框架。
- 演示 Kafka、RocketMQ、RabbitMQ 等消息系统与 MongoDB、Lucene、Web3j 等数据技术。
- 少量 Python 辅助脚本（示例：Netty 场景实验）。

## 快速开始
### 环境要求
- JDK 25 及以上
- Apache Maven 3.8+（仓库未提供 Maven Wrapper）
- 可选：Docker 或本地安装的中间件（Kafka、RabbitMQ、MongoDB、Redis、Nacos 等）

### 克隆与全量构建
```bash
git clone <repo-url>
cd haifa
mvn -T1C clean install
```

### 单模块构建或测试
```bash
mvn -pl haifa-<module> -am clean test
```
通过 `-pl`（project list）限制构建范围；如只需编译可追加 `-DskipTests`。

### 运行示例注意事项
- 多数模块依赖本地服务，运行前请确认对应中间件已启动，或根据源码调整为 Mock/嵌入式方案。
- 部分旧实验固定使用较老版本的 Servlet 容器（Tomcat 7、Jetty 8），升级前请充分验证兼容性。
- 生成文件位于各模块的 `target/` 目录，需要在 `.gitignore` 中保持忽略。

## 模块协作建议
- 尽量新增独立示例而非大规模重构，仓库定位为技术练习场。
- 新主题遵循 `haifa-<topic>` 命名，并记得在父 `pom.xml` 中注册模块。
- 外部依赖与环境变量请在模块内部文档或注释中说明，可补充 README。
- 修改依赖后建议执行 `mvn -pl <module> -am verify`，避免全仓库重编译。

## 文档索引
- `README.md` – 英文版说明
- `AGENTS.md` – 面向自动化助手的工作提示
- 模块内 README（如果存在）– 主题详细说明

## 贡献指南
- 保证示例可复现：只提交必要的源码与配置文件。
- 处理遗留问题时可使用 TODO 或 Issue 记录后续工作，而非提交未完成的迁移。
- 遵循既有代码风格（`org.wrj.<topic>` 包结构，类名语义清晰，谨慎使用额外注解）。

## 许可证
本项目使用 MIT License，详见 `LICENSE`。
