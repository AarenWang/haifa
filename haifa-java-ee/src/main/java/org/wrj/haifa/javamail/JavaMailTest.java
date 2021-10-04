package org.wrj.haifa.javamail;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Created by wangrenjun on 2017/5/12.
 */
public class JavaMailTest {

    public static void main(String[] args) {
        // 收件人电子邮箱
        String to = "aaa@qq.com";

        // 发件人电子邮箱
        String from = "bbb@qq.com";

        // 指定发送邮件的主机为 smtp.qq.com
        String host = "smtp.qq.com"; // QQ 邮件服务器

        String password= "temp_password";

        // 获取系统属性
        Properties properties = System.getProperties();

        // 设置邮件服务器
        properties.setProperty("mail.smtp.host", host);

        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.socketFactory.port", "465");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.socketFactory.fallback", "false");
        properties.put("mail.smtp.starttls.enable", "false");
        //properties.put("mail.smtp.ssl.trust", "smtp.qq.com");
        // 获取默认session对象
        Session session = Session.getDefaultInstance(properties, new Authenticator() {

            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password); // 发件人邮件用户名、密码
            }
        });

        session.setDebug(true);

        try {
            // 创建默认的 MimeMessage 对象
            MimeMessage message = new MimeMessage(session);

            // Set From: 头部头字段
            message.setFrom(new InternetAddress(from));

            // Set To: 头部头字段
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));


            // Set Subject: 头部头字段
            message.setSubject("This is the Subject Line!");

            // 设置消息体
            message.setText("This is actual message");

            // 发送消息
            Transport.send(message);
            System.out.println("Sent message successfully....from w3cschool.cc");
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }
}
