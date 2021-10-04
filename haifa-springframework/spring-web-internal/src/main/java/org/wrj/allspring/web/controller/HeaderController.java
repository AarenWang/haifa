package org.wrj.allspring.web.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

@RestController
@RequestMapping("/head")
public class HeaderController {

    @RequestMapping("/showheader")
    public String index(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        Enumeration<String> enumeration = request.getHeaderNames();
        while (enumeration.hasMoreElements()){
            String header = enumeration.nextElement();
            String value = request.getHeader(header);
            sb.append(header+":"+value+"<br/>");

        }
        return sb.toString();
    }
}
