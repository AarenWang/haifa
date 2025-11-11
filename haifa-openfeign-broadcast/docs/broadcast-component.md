# OpenFeign 广播调用组件说明

## 1. 使用指南

### 引入依赖
将 `haifa-openfeign-broadcast-extension` 模块发布或以 Maven 坐标的形式引入到业务项目中，该模块已经声明了对公共 RPC 契约以及 Spring Cloud OpenFeign 的依赖，开箱即可使用。【F:haifa-openfeign-broadcast/openfeign-extension/pom.xml†L9-L29】

### 启用客户端
1. 在 Spring Boot 应用中启用服务发现与 Feign，示例客户端通过 `@EnableDiscoveryClient` 和 `@EnableFeignClients` 激活 Nacos 注册中心与 RPC 接口扫描。【F:haifa-openfeign-broadcast/broadcast-client/src/main/java/org/wrj/haifa/openfeign/broadcast/client/BroadcastClientApplication.java†L7-L16】
2. 在业务 Feign 接口的方法上标注 `@FeignBroadcast` 注解，控制是否遇到错误立即终止调用。示例中的 `BroadcastMessageClient#broadcast` 将失败实例的信息也聚合返回。【F:haifa-openfeign-broadcast/rpc/src/main/java/org/wrj/haifa/openfeign/broadcast/rpc/BroadcastMessageClient.java†L9-L14】
3. 发起请求时使用普通的 Feign 客户端调用即可，返回值为聚合后的 `BroadcastResult`，其中包含所有实例的状态明细，方便业务自定义处理逻辑。【F:haifa-openfeign-broadcast/rpc/src/main/java/org/wrj/haifa/openfeign/broadcast/rpc/BroadcastResult.java†L1-L12】【F:haifa-openfeign-broadcast/openfeign-extension/src/main/java/org/wrj/haifa/openfeign/broadcast/core/BroadcastFeignClient.java†L60-L104】

### 配套服务端
广播调用要求目标服务实现幂等接口。示例 Provider 在每次请求时记录消息并返回 `MessageAck`，方便客户端确认各实例是否写入成功。【F:haifa-openfeign-broadcast/broadcast-server/src/main/java/org/wrj/haifa/openfeign/broadcast/server/MessageController.java†L18-L47】【F:haifa-openfeign-broadcast/openfeign-extension/src/main/java/org/wrj/haifa/openfeign/broadcast/shared/MessageAck.java†L1-L12】

## 2. 实现原理

1. **注解驱动的广播标记**：`@FeignBroadcast` 通过 `failFast` 参数声明广播策略，扩展在拦截器中读取该注解并写入内部 Header，标记当前请求需要广播执行。【F:haifa-openfeign-broadcast/rpc/src/main/java/org/wrj/haifa/openfeign/broadcast/rpc/FeignBroadcast.java†L1-L20】【F:haifa-openfeign-broadcast/openfeign-extension/src/main/java/org/wrj/haifa/openfeign/broadcast/core/FeignBroadcastRequestInterceptor.java†L8-L30】
2. **自定义 Feign Client**：`FeignBroadcastBeanPostProcessor` 拦截 Spring 容器中的 Feign `Client` Bean，包装成 `BroadcastFeignClient`，在执行时根据服务发现拿到全部实例并循环调用，同时聚合返回体和错误信息。【F:haifa-openfeign-broadcast/openfeign-extension/src/main/java/org/wrj/haifa/openfeign/broadcast/core/FeignBroadcastBeanPostProcessor.java†L8-L27】【F:haifa-openfeign-broadcast/openfeign-extension/src/main/java/org/wrj/haifa/openfeign/broadcast/core/BroadcastFeignClient.java†L33-L156】
3. **结果聚合与降级策略**：广播客户端为每个实例构建 `InstanceResult`，统计状态码、是否成功以及响应体，最终封装到 `BroadcastResult` 并返回 200 或 207 状态码，既能体现整体成功也能暴露单点失败详情。【F:haifa-openfeign-broadcast/openfeign-extension/src/main/java/org/wrj/haifa/openfeign/broadcast/core/BroadcastFeignClient.java†L70-L156】【F:haifa-openfeign-broadcast/rpc/src/main/java/org/wrj/haifa/openfeign/broadcast/rpc/InstanceResult.java†L1-L11】

## 3. OpenFeign 扩展点与 Spring 集成

1. **OpenFeign RequestInterceptor**：通过实现 `RequestInterceptor`，在模板渲染阶段动态添加内部 Header，这也是 OpenFeign 提供的标准扩展点之一。【F:haifa-openfeign-broadcast/openfeign-extension/src/main/java/org/wrj/haifa/openfeign/broadcast/core/FeignBroadcastRequestInterceptor.java†L8-L30】
2. **OpenFeign Client 扩展**：`BroadcastFeignClient` 实现了 Feign 的 `Client` 接口，自行处理 HTTP 调度与响应聚合，确保广播逻辑对业务透明。【F:haifa-openfeign-broadcast/openfeign-extension/src/main/java/org/wrj/haifa/openfeign/broadcast/core/BroadcastFeignClient.java†L31-L156】
3. **Spring BeanPostProcessor**：借助 Spring `BeanPostProcessor` 扩展点，在 Feign 默认客户端创建完成后进行替换，而不会影响其它 Bean，这也是将 OpenFeign 扩展无侵入集成到 Spring Boot 应用的关键。【F:haifa-openfeign-broadcast/openfeign-extension/src/main/java/org/wrj/haifa/openfeign/broadcast/core/FeignBroadcastBeanPostProcessor.java†L8-L27】
4. **自动配置装配**：`FeignBroadcastConfiguration` 以 Spring `@Configuration` 形式暴露拦截器与 BeanPostProcessor，只要引入模块即可完成自动装配，实现与业务代码的低耦合。【F:haifa-openfeign-broadcast/openfeign-extension/src/main/java/org/wrj/haifa/openfeign/broadcast/core/FeignBroadcastConfiguration.java†L1-L21】
