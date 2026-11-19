package org.wrj.allspring.version4.rpc;

import org.springframework.stereotype.Service;

/**
 * RPC 服务端实现，模拟真实的远程服务提供者。
 */
@Service
public class UserServiceImpl implements UserService {

    @Override
    public String getUserInfo(Long userId) {
        return "UserInfo{userId=" + userId + ", name=Spring}";
    }

    @Override
    public boolean updateUserName(Long userId, String userName) {
        System.out.println("[UserService Server] update userId=" + userId + " to userName=" + userName);
        return true;
    }
}
