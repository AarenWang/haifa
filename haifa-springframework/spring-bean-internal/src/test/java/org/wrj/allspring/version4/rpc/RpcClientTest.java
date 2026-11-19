package org.wrj.allspring.version4.rpc;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 验证 {@link EnableMyRpcClients} 扫描注入的客户端代理，能否转发到服务端实现。
 * <p>
 * 先启动 Server 端（{@link RpcServerConfig}），再启动 Client 端（{@link RpcClientConfig}），
 * 最后通过客户端代理调用方法，观察是否执行到 {@link UserServiceImpl}。
 */
public class RpcClientTest {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                RpcServerConfig.class,
                RpcClientConfig.class
        );

        UserService userService = context.getBean("userService", UserService.class);
        System.out.println("Client bean type: " + userService.getClass().getName());

        String userInfo = userService.getUserInfo(1L);
        System.out.println("Client call result: " + userInfo);

        boolean updated = userService.updateUserName(1L, "Spring");
        System.out.println("Client call result: " + updated);

        context.close();
    }
}
