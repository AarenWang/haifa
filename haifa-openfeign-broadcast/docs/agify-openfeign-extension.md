# 使用 OpenFeign 调用公共 HTTPS API 的示例

本文示例展示了如何通过 OpenFeign 的扩展点，将公共互联网上的 HTTPS API —— [Agify](https://api.agify.io) —— 封装成一个具备请求/响应 Java 对象定义的客户端接口。

## 1. API 简介

Agify 服务通过名字（可选指定国家）预测该名字对应人群的平均年龄。示例请求：

```http
GET https://api.agify.io/?name=michael&country_id=US
```

响应结果为：

```json
{
  "name": "michael",
  "age": 69,
  "count": 145561,
  "country_id": "US"
}
```

## 2. 请求与响应对象

在 `openfeign-extension` 模块中新增了 `AgifyRequest` 与 `AgifyResponse` 两个对象，分别对应 HTTP 查询参数以及返回的 JSON 结构：

- `AgifyRequest`：只读数据类，包含 `name` 与可选的 `countryId` 字段。
- `AgifyResponse`：包含 `name`、`age`、`count` 以及 `countryId` 字段，并通过 `@JsonProperty` 将蛇形命名转换为驼峰命名。

## 3. OpenFeign 接口定义

`AgifyClient` 使用 `@FeignClient` 注解，指向 Agify 的基础地址，并声明为 GET 请求：

```java
@GetMapping("/")
AgifyResponse predictAge(@SpringQueryMap AgifyRequest request);
```

为了让请求对象直接转为查询字符串参数，客户端注册了自定义的 `QueryMapEncoder`。

## 4. 扩展点：自定义 QueryMapEncoder

默认情况下，`SpringQueryMap` 会将 Java 属性名直接输出为查询参数（驼峰命名）。Agify API 期望蛇形命名，例如 `country_id`。通过在 `AgifyClientConfiguration` 中注册 Bean：

```java
@Bean
public QueryMapEncoder agifyQueryMapEncoder() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    return new JacksonSnakeCaseQueryMapEncoder(mapper);
}
```

我们利用 Jackson 将请求对象转换为 Map，并过滤空值，从而达成 Java 对象 -> Query 参数的自定义映射。

此外示例还展示了如何通过注册 `Logger.Level` Bean 来控制 Feign 的日志级别。

## 5. 在 Spring Boot 应用中使用

在应用中引入 `haifa-openfeign-broadcast-extension` 依赖后，可以通过以下方式启用客户端：

```java
@EnableFeignClients(clients = AgifyClient.class)
@SpringBootApplication
public class DemoApplication { }
```

随后即可在业务组件中注入 `AgifyClient` 并传入 `AgifyRequest`：

```java
AgifyResponse response = agifyClient.predictAge(new AgifyRequest("michael", "US"));
```

这样就完成了基于 OpenFeign 扩展点的公共 HTTPS API 封装。
