package org.wrj.haifa.mail;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * 邮件发送工具类
 * @author think
 *
 */
public class MailSender {
	
	private static final String emailRegex = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
	private static List<String> toAddList = new ArrayList<String>();
    private static List<String> ccAddList = new ArrayList<String>();
    private static List<String> bccAddList = new ArrayList<String>();
    private static Map<String, String> attachFile = new HashMap<String,String>();
	
	public static void sendMail(String subject,String content) throws Exception{
		ResourceBundle rb = ResourceBundle.getBundle("org/wrj/mail/mail",Locale.getDefault());  
        Properties props = new Properties();
        String protocol = rb.getString("mail.transport.protocol");
        String host = rb.getString("mail.smtp.host");
        String port = rb.getString("mail.smtp.port");
        String from = rb.getString("mail.host.user.from");
        String user = rb.getString("mail.host.user");
        String password = rb.getString("mail.host.password");
        
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.transport.protocol", protocol);
        
        boolean isAutheticate = "true".equals(rb.getString("mail.smtp.auth"));
        boolean isDebug = "true".equals(rb.getString("mail.send.debug"));
        if(isAutheticate){
        	 props.put("mail.smtp.auth", "true");
        }
        Session session = Session.getDefaultInstance(props);
        session.setDebug(isDebug);
        
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        for(String to:toAddList){
        	message.addRecipient(Message.RecipientType.TO,new InternetAddress(to));
        }
        for(String to:ccAddList){
        	message.addRecipient(Message.RecipientType.CC,new InternetAddress(to));
        }
        for(String to:bccAddList){
        	message.addRecipient(Message.RecipientType.BCC,new InternetAddress(to));
        }
        
        message.setSubject(MimeUtility.encodeText(subject));
        
        Multipart multipart = new MimeMultipart();
        BodyPart contentPart = new MimeBodyPart();
        contentPart.setContent(content, "text/html;charset=GBK");
        multipart.addBodyPart(contentPart);
        
        // 发送附件
        Set<Entry<String,String>> sets = attachFile.entrySet();
        for(Entry<String,String> entry:sets){
        	String attachName = entry.getKey();
        	String attachPath = entry.getValue();
        	BodyPart attachmentPart= new MimeBodyPart();
            DataSource source = new FileDataSource(attachPath);
            attachmentPart.setDataHandler(new DataHandler(source));
            attachmentPart.setFileName(MimeUtility.encodeText(attachName));
            multipart.addBodyPart(attachmentPart);
        }
       
        message.setContent(multipart);
        message.saveChanges();
        Transport transport = session.getTransport(protocol);
        transport.connect(host,user,password);
        transport.sendMessage(message, message.getAllRecipients());
        transport.close();
    }
	
    
    public static void  addToAddres(String address){
    	checkEmail(address);
    	toAddList.add(address);
    }
    
    public static void addCCAddres(String address){
    	ccAddList.add(address);
    }
    
    public static void addBCCAddres(String address){
    	bccAddList.add(address);
    }
    
    public static void addAttachFile(String attachName,String path) {
    	attachFile.put(attachName, path);
		
	}
    
    private static void checkEmail(String mail){
    	if(mail == null){
    		throw new NullPointerException("email地址不能为空");
    	}
    	if(emailRegex.matches(mail)){
    		throw new IllegalArgumentException("email地址["+mail+"]不符合格式");
    	}
    }
}

