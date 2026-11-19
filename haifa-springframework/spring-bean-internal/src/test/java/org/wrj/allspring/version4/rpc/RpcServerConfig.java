package org.wrj.allspring.version4.rpc;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * RPC 服务端配置，负责扫描并注册 {@link UserService} 的真实实现。
 */
@Configuration
@ComponentScan(basePackages = "org.wrj.allspring.version4.rpc")
public class RpcServerConfig {
}
