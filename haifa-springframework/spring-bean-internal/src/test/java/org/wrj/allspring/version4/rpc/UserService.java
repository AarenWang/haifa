package org.wrj.allspring.version4.rpc;

/**
 * 示例 RPC 客户端接口，用于演示 {@link EnableMyRpcClients} 的自动扫描与代理注入能力。
 */
@MyRpcClient
public interface UserService {

    String getUserInfo(Long userId);

    boolean updateUserName(Long userId, String userName);
}
