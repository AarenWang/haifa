package org.wrj.allspring.web.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by wangrenjun on 2017/10/17.
 */
@RestController
@RequestMapping("/")
public class IndexController {

    @RequestMapping("")
    public String index() {
        return "Index Page";
    }
}
