package org.wrj.haifa.mail;


public class MailTest {
	public static void main(String[] args){

		    MailSender.addToAddres("to_email@qq.com");
	        
	        String title = "亲爱的 凤凰网读者您好";//邮件的标题
	        String content = "<h1>感谢您对凤凰网的支持<a href='http://www.ifeng.com'>请点击</a> <h1>";//邮件正文
	        
	        MailSender.addAttachFile("附件1.jpg", "D:/test.jpg");
	        MailSender.addAttachFile("附件2.jpg", "D:/logo.jpg");
	        try {
	        	MailSender.sendMail(title,content);
		        System.out.println("发送成功");
	        } catch (Exception e){
	            e.printStackTrace();
	        }
	    }

	}

