# Java 21 结构化并发（Structured Concurrency）

> **Java 21 状态：**`StructuredTaskScope` 是 JDK 21 的预览 API（JEP 453）。编译和运行都必须传入 `--enable-preview`，且升级 JDK 时应重新检查 API；预览 API 不承诺保持兼容。

## 技术背景：管理“同一次请求”的并发任务

商品详情页往往需要同时读取商品、库存和价格。`ExecutorService`、`CompletableFuture` 都能创建并发任务，但它们很容易脱离发起请求的方法：父请求已超时或失败，子任务还在执行；一个关键任务失败后，其余任务也不会自动停止；取消、超时、异常处理散落在 future 回调中。

结构化并发把相关子任务限制在一个词法作用域中：在 scope 内启动（`fork`）任务、等待（`join`）并取结果；离开 `try-with-resources` 时，未结束的任务会被关闭。这样子任务与父任务形成清晰的生命周期嵌套关系。

Java 21 同时正式发布了虚拟线程。`StructuredTaskScope` 默认用虚拟线程运行子任务，特别适合 HTTP、数据库和 RPC 这类**阻塞型 I/O** 聚合。它不会让 CPU 密集型计算自动更快；这类工作仍应按处理器核心数控制并行度。

## JDK API 速览

| API | 用途 |
| --- | --- |
| `StructuredTaskScope<T>` | 管理一组子任务的 scope 基类。 |
| `fork(Callable<? extends T>)` | 启动子任务，返回 `StructuredTaskScope.Subtask<T>`。 |
| `join()` | 等待子任务结束或 scope 被关闭。 |
| `joinUntil(Instant)` | 等待到绝对截止时间；超时抛出 `TimeoutException`。 |
| `Subtask.get()` | 读取成功任务的结果；应在 `join` 后使用。 |
| `ShutdownOnFailure` | 任一任务失败时关闭 scope，适合完整聚合。 |
| `ShutdownOnSuccess<T>` | 第一个任务成功时关闭 scope，适合主备竞速。 |
| `throwIfFailed()` | 将子任务失败传播给父任务。 |

固定使用顺序是：**fork → join / joinUntil → 检查失败或读结果 → close**。始终使用 `try-with-resources`，确保异常、超时和提前返回时也能清理子任务。

## 例子一：商品页聚合，任意失败即取消

商品、库存、价格缺一不可，所以使用 `ShutdownOnFailure`。任一服务失败时，其他还未完成的调用会被请求取消，避免无效资源消耗。

```java
import java.util.concurrent.StructuredTaskScope;

public final class ProductPageService {
    record Product(String id, String name) {}
    record Stock(String productId, int quantity) {}
    record Price(String productId, long cents) {}
    record ProductPage(Product product, Stock stock, Price price) {}

    ProductPage load(String productId) throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var product = scope.fork(() -> productClient.get(productId));
            var stock = scope.fork(() -> inventoryClient.get(productId));
            var price = scope.fork(() -> pricingClient.get(productId));

            scope.join().throwIfFailed();
            return new ProductPage(product.get(), stock.get(), price.get());
        }
    }

    // 为突出并发逻辑，省略三个 client 的具体实现。
    private final ProductClient productClient = null;
    private final InventoryClient inventoryClient = null;
    private final PricingClient pricingClient = null;
}
```

`throwIfFailed()` 让失败回到父任务的正常异常处理路径；只有它没有抛异常时，才读取 `Subtask.get()`。这比 `CompletableFuture.allOf(...)` 更直接地将失败策略、等待点和清理逻辑放在同一作用域内。

## 例子二：主备服务竞速，第一个成功结果获胜

读取配置时，主服务短暂故障不应让请求直接失败。并发请求主、备服务，目标是尽快取得**第一个成功**的值，因此使用 `ShutdownOnSuccess`。

```java
import java.util.concurrent.StructuredTaskScope;

public final class ConfigReader {
    String readFeatureFlag() throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
            scope.fork(() -> primaryConfigClient.read("new-checkout"));
            scope.fork(() -> backupConfigClient.read("new-checkout"));

            scope.join();
            // 首个成功结果；全部失败时 result() 抛异常。
            return scope.result();
        }
    }

    private final ConfigClient primaryConfigClient = null;
    private final ConfigClient backupConfigClient = null;
}
```

成功任务确定后，scope 会关闭并取消其余尚未完成的竞争任务。取消是协作式的：子任务必须正确响应中断，不能假设任意计算都能被强制停止。

## 例子三：为整个聚合设置截止时间

超时应覆盖整个并发操作，而不是把每个服务的超时相加。`joinUntil` 接受绝对截止时间；即使等待超时，离开 `try` 块时也会清理子任务。

```java
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeoutException;

String loadWithDeadline() throws Exception {
    Instant deadline = Instant.now().plus(Duration.ofMillis(800));
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        var account = scope.fork(accountClient::load);
        var permissions = scope.fork(permissionClient::load);
        try {
            scope.joinUntil(deadline).throwIfFailed();
            return account.get() + ":" + permissions.get();
        } catch (TimeoutException timeout) {
            throw new IllegalStateException("request timed out", timeout);
        }
    }
}
```

## 编译与运行

JDK 21 中这是预览功能，因此 `javac` 和 `java` 都要开启预览：

```bash
javac --release 21 --enable-preview ProductPageService.java
java --enable-preview ProductPageService
```

Maven 中，编译插件需传递 `--enable-preview`：

```xml
<configuration>
    <release>21</release>
    <compilerArgs><arg>--enable-preview</arg></compilerArgs>
</configuration>
```

测试运行器（Surefire/Failsafe）也必须配置：

```xml
<configuration><argLine>--enable-preview</argLine></configuration>
```

本目录只提供 README 和可复制的代码片段，避免现有模块在未启用 Preview 的构建中因引用该 API 而失败。

## 实践建议

1. 一个 scope 只管理同一个请求或业务操作的子任务，不要把它当全局任务池。
2. 需要全部结果时选择 `ShutdownOnFailure`；多副本竞速时选择 `ShutdownOnSuccess`。
3. 子任务应正确处理 `InterruptedException`，使取消尽快生效。
4. 将时间预算放在父 scope 的 `joinUntil`，表达整个操作的截止时间。
5. 在生产使用前确认团队能够锁定 JDK 版本，并接受预览 API 未来可能变更的风险。
