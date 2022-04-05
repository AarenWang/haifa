package org.wrj.haifa.javamail;

import org.apache.commons.cli.*;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Created by wangrenjun on 2017/5/12.
 */
public class JavaMailTest {

    public static void main(String[] args) throws ParseException {
        SendConfig sendConfig = parse(args);
        // 收件人电子邮箱
        String to = sendConfig.getTo();

        // 发件人电子邮箱
        String from = sendConfig.getFrom();

        //  发送邮箱服务器认证用户
        String username = sendConfig.getUsername();

        // 指定发送邮件的主机为 smtp.qq.com
        String host = "smtp.qq.com"; // QQ 邮件服务器

        String password= sendConfig.getPassword();

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
                return new PasswordAuthentication(username, password); // 发件人邮件用户名、密码
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
            message.setHeader("Content-Type","text/html; charset=UTF-8");

            // 设置消息体
            // message.setText("This is actual message 中文会乱码吗 <font color=\"red\">这个是什么颜色呀</font>");
            message.setContent("This is actual message 中文会乱码吗 <font color=\"red\">这个是什么颜色呀</font>","text/html; charset=UTF-8");

            // 发送消息
            Transport.send(message);
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }


    private static SendConfig parse(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("h", false, "usage help");
        options.addOption("help", false, "usage help");
        options.addOption("f",true,"mail from");
        options.addOption("t",true,"mail to");
        options.addOption("u",true,"mail send username");
        options.addOption("p",true,"mail send password");


        CommandLineParser paraer = new BasicParser();
        CommandLine cmdLine = paraer.parse(options, args);
        if (cmdLine.hasOption("h") || cmdLine.hasOption("help") || !cmdLine.hasOption("f") || !cmdLine.hasOption("t")  || !cmdLine.hasOption("u") || !cmdLine.hasOption("p")){
            HelpFormatter hf = new HelpFormatter();
            hf.setWidth(110);
            hf.printHelp("JavaMailTest",options);
        }

        SendConfig sc = new SendConfig();
        sc.setFrom(cmdLine.getOptionValue("f"));
        sc.setTo(cmdLine.getOptionValue("t"));
        sc.setPassword(cmdLine.getOptionValue("p"));
        sc.setUsername(cmdLine.getOptionValue("u"));
        return sc;
    }
}



class SendConfig {
    private String from;

    private String to;

    private String username;

    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
