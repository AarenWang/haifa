package org.wrj.allspring.web.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by wangrenjun on 2017/11/2.
 */
public class DownloadFileServlet extends HttpServlet{

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {


        InputStream is = new FileInputStream("/Users/wangrenjun/Desktop/去花双11梳理.xlsx");
        org.apache.commons.io.IOUtils.copy(is, response.getOutputStream());
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=abc.xlsx");
        response.getOutputStream().flush();
    }
}
