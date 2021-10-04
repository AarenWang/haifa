package org.wrj.allspring.web.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.wrj.allspring.version4.hook.UserModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangrenjun on 2017/10/17.
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @RequestMapping("/list")
    @ResponseBody
    public List<UserModel> list(){

        List<UserModel> list = new ArrayList<UserModel>();
        list.add(new UserModel("Jack",20));

        list.add(new UserModel("Zuck",29));

        list.add(new UserModel("Charles",50));

        return list;

    }

}
