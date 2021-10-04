package org.wrj.allspring.version4.hook;

import org.springframework.stereotype.Service;

/**
 * Created by wangrenjun on 2017/3/14.
 */
@Service
public class UserServiceImpl implements  UserService{


    public void createUser(String userName, String password) {
        System.out.print("UserServiceImpl.createUser");
    }
}
