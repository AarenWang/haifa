package org.wrj.haifa.servlet4;


import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.PushBuilder;
import java.io.PrintWriter;


@WebServlet(value = {"/http2"})
public class Http2Servlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
//        PushBuilder pushBuilder = req.newPushBuilder();
//        pushBuilder.path("assets/steven_jobs.jpg")
//                .addHeader("content-type", "image/jpeg")
//                .push();
//        try{
//            PrintWriter respWriter = resp.getWriter();
//            respWriter.write("<html>" +
//                    "<img src='assets/steven_jobs.jpg'>" +
//                    "</html>");
//        }catch (Exception e){
//            e.printStackTrace();
//        }
    }
}
