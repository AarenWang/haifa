package org.wrj.allspring.web.controller;

import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@RestController
@RequestMapping("/health")
public class HealthV1Controller {

    @RequestMapping("/check")
    public void healthCheck(HttpServletResponse response,ModelMap model) throws IOException {
        response.setContentType("text/html;charset=utf-8");
        PrintWriter out = response.getWriter();
        out.write("HealthV1Controller.healthCheck");
        out.flush();
        out.close();

    }


}
