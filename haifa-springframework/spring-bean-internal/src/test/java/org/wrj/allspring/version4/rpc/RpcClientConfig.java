package org.wrj.allspring.version4.rpc;

import org.springframework.context.annotation.Configuration;

/**
 * 开启 MyRpcClient 扫描的示例配置类。
 */
@Configuration
@EnableMyRpcClients(basePackages = "org.wrj.allspring.version4.rpc")
public class RpcClientConfig {
}
