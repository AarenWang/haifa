package org.wrj.haifa.springsecurity.web.controller;

import com.ramostear.captcha.HappyCaptcha;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class CaptchaController {

    @GetMapping("/captcha")
    public void happyCaptcha(HttpServletRequest request, HttpServletResponse response){
        HappyCaptcha.require(request,response).build().finish();
    }


    @PostMapping("/verify")
    public String verify(String code,HttpServletRequest request){
        //Verification Captcha
        boolean flag = HappyCaptcha.verification(request,code,true);
        if(flag){
            //Other operations...
        }
    }
}
