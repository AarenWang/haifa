# Automated Agent Guide for Haifa

## Repository Snapshot
- Purpose: sandbox for studying Java ecosystem topics—frameworks, middleware, algorithms, and infrastructure demos.
- Structure: single parent `pom.xml` aggregating 40+ Maven modules; most code lives under `org.wrj`.
- Tooling: Java 25, Maven 3.x, occasional Python helper scripts, and Spring Boot 2.5.x baselines.
- License: MIT (`LICENSE`), so derived work must preserve copyright and license notice.

## Module Map (high level)
- Core foundations: `haifa-base`, `haifa-java-library`, `haifa-my-library`, `haifa-jvm-internal`, `haifa-legacy`, `haifa-block`, `haifa-vs`.
- Concurrency & coordination: `haifa-concurrent`, `haifa-scheduler`, `haifa-coordinate`, `haifa-reactor`.
- Framework deep dives: `haifa-springframework`, `haifa-springboot`, `haifa-springcloud-nacos`, `haifa-springsecurity`, `haifa-dubbo`, `haifa-web-container`, `haifa-java-ee`, `haifa-template-engine`, `haifa-markup-language`.
- Integration & middleware: `haifa-mq` (Kafka/RabbitMQ/RocketMQ), `haifa-cache`, `haifa-httpclient`, `haifa-aws`, `haifa-websocket`, `haifa-grpc`, `haifa-netty`.
- Data & search: `haifa-mongodb`, `haifa-search`, `haifa-json`, `haifa-nlp`, `haifa-web3j`.
- Big data & streaming: `haifa-flink`, `haifa-flink13`.
- Patterns, testing, and algorithms: `haifa-design-pattern`, `haifa-junit5`, `haifa-leetcode`.
- AI experiments: `haifa-ai` (Alibaba Cloud models, Spring integrations).
- Tooling & DevOps: `haifa-ci-cd`, `haifa-empty` (scaffold).

## Key Workflows
- Build everything: `mvn -T1C clean install` from the repository root. Expect long dependency resolution on first run.
- Build or test one module: `mvn -pl haifa-<module> -am clean test` to keep scope tight.
- IDE setup: import as a Maven project; ensure compiler level 25 and Lombok (where used) are configured.

## Environment Notes
- Many demos expect local services (e.g., MongoDB, Kafka/RabbitMQ, Redis-style caches, AWS credentials, Nacos). Stub or mock these when writing automated tests.
- Some legacy modules pin very old servlet containers (Tomcat 7, Jetty 8). Avoid upgrading blindly—verify compatibility first.
- Generated `target/` directories exist across modules; do not check agent output into them.
- External configs often live in `src/main/resources`. Preserve property names when extending examples.

## Automation Guidelines
- Prefer focused contributions (documentation, isolated bug fixes, new samples) over sweeping refactors; modules are intentionally heterogenous.
- When introducing new experiments, clone the existing module pattern: create a new `haifa-*` module and add it to the parent `pom.xml`.
- Keep demos self-contained—document any required infrastructure and provide fallbacks (embedded brokers, testcontainers, or mocks) where feasible.
- Uphold Java code style already present (package naming `org.wrj.*`, minimal annotations, descriptive class names).
- Validate builds with `mvn -pl <module> -am verify` whenever dependencies change; skip integration-heavy modules if services are unavailable.

## Open Ends & Caution
- Integration tests are sparse; add unit tests when extending logic-heavy areas (see `haifa-base` for examples).
- Security-sensitive demos (cryptology, AWS, Web3) may contain placeholder credentials—double-check before committing changes.
- Large dependency upgrades can ripple across modules relying on the shared dependency management block in the root `pom.xml`; audit impact module-by-module.

## Suggested Agent Tasks
- Improve module-level README coverage (most modules lack an overview or setup instructions).
- Add minimal automated tests or mocks for demos that currently require live infrastructure.
- Update dependency versions carefully (Spring, Flink, RocketMQ) while validating compatibility.
- Expand AI module docs with clear usage steps and model requirements.
