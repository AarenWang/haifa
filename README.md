# Haifa

A multi-module Maven workspace for exploring the Java ecosystem—from core language features to frameworks, middleware, distributed systems, and AI integrations. Each `haifa-*` module is a self-contained demo or study note that documents experiments, patterns, and lessons learned while revisiting technologies used in day-to-day development.

## Repository Overview
- Parent build: single `pom.xml` at the root orchestrates 40+ modules, all targeting Java 17.
- Naming: modules follow the `haifa-<topic>` convention (e.g., `haifa-concurrent`, `haifa-springframework`).
- Source layout: primary packages live under `org.wrj` with accompanying resources in `src/main/resources`.
- License: MIT; retain the copyright header when extending.

## Module Landscape
- Foundations & language internals: `haifa-base`, `haifa-java-library`, `haifa-my-library`, `haifa-jvm-internal`, `haifa-legacy`, `haifa-block`, `haifa-vs`.
- Concurrency & coordination: `haifa-concurrent`, `haifa-scheduler`, `haifa-coordinate`, `haifa-reactor`.
- Framework deep dives: `haifa-springframework`, `haifa-springboot`, `haifa-springcloud-nacos`, `haifa-springsecurity`, `haifa-dubbo`, `haifa-java-ee`, `haifa-web-container`, `haifa-template-engine`, `haifa-markup-language`.
- Integration & middleware: `haifa-mq` (Kafka/RabbitMQ/RocketMQ demos), `haifa-cache`, `haifa-httpclient`, `haifa-aws`, `haifa-websocket`, `haifa-grpc`, `haifa-netty`.
- Data, search & blockchain: `haifa-mongodb`, `haifa-search`, `haifa-json`, `haifa-nlp`, `haifa-web3j`.
- Big data & streaming: `haifa-flink`, `haifa-flink13`.
- Patterns, testing & algorithms: `haifa-design-pattern`, `haifa-junit5`, `haifa-leetcode`.
- AI experiments: `haifa-ai-alibaba`, `haifa-ai-spring`.
- Tooling & CI: `haifa-ci-cd`, `haifa-empty`.

> Tip: Not every module has a README yet—scan package names or tests for context when exploring a new area.

## Technology Highlights
- Java 17 toolchain with Maven 3.x builds.
- Spring ecosystem (Framework, Boot, Cloud, Security), Netty, Dubbo, Reactor, Akka, Flink, and more.
- Messaging stacks (Kafka, RocketMQ, RabbitMQ) and database clients (MongoDB, Lucene, Web3j).
- Occasional Python helpers (e.g., simple Netty integrations) alongside Java code.

## Getting Started
### Prerequisites
- Java Development Kit 17+
- Apache Maven 3.8+ (wrapper not included)
- Optional: Docker/locally installed services for middleware demos (Kafka, RabbitMQ, MongoDB, Redis, Nacos, etc.)

### Clone & Build Everything
```bash
git clone <repo-url>
cd haifa
mvn -T1C clean install
```

### Build or Test a Single Module
```bash
mvn -pl haifa-<module> -am clean test
```
Use `-pl` (project list) to narrow the scope; add `-DskipTests` when exploring compile-only changes.

### Running Samples
- Many modules expect local infrastructure—consult source comments before running.
- Legacy experiments pin older dependencies (e.g., Tomcat 7, Jetty 8); keep them isolated unless you need compatibility updates.
- Generated content lives in `target/`; ignore it in version control.

## Working With Modules
- Prefer adding focused demos instead of sweeping refactors; the repository is intentionally a playground for isolated studies.
- When introducing a new topic, follow the naming convention (`haifa-<topic>`) and add the module to the parent `pom.xml`.
- Document external prerequisites and configuration in the module itself (consider adding a module-level README).
- Validate changes with `mvn -pl <module> -am verify` to avoid rebuilding the whole tree.

## Documentation Map
- `AGENTS.md` – automation-oriented notes for coding assistants.
- `README-zh.md` – Chinese version of this document.
- Module READMEs (where available) – topic-specific instructions.

## Contributing
- Keep experiments reproducible: check in only source/configuration needed to rebuild examples.
- Leave TODO comments or issues for follow-up work rather than half-complete migrations.
- Respect existing code style (`org.wrj.<topic>` packages, descriptive class names, minimal annotations unless required).

## License
This project is released under the MIT License. See `LICENSE` for details.
