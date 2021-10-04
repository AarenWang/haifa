package org.wrj.haifa;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;


public class QueueSend {   
  
	//  ConnectionFactory-0
	//  Queue-0
	  
    // Defines the JNDI context factory.   
    public final static String JNDI_FACTORY = "weblogic.jndi.WLInitialContextFactory";   
  
    // Defines the JNDI provider url.   
    public final static String PROVIDER_URL = "t3://localhost:7521";   
  
    // Defines the JMS connection factory for the queue.   
    public final static String JMS_FACTORY = "ConnectionFactory-0";   
  
    // Defines the queue �õ��Ƕ�Ӧ QUEUE��JNDI����
    public final static String QUEUE = "Queue-0";   
  
    private QueueConnectionFactory qconFactory;   
  
    private QueueConnection qcon;   
  
    private QueueSession qsession;   
  
    private QueueSender qsender;   
  
    private Queue queue;   
  
    private TextMessage msg;   
  
    private StreamMessage sm;   
  
    private BytesMessage bm;   
  
    private MapMessage mm;   
  
    private ObjectMessage om;   
  
    /**  
     * Creates all the necessary objects for sending messages to a JMS queue.  
     *   
     * @param ctx  
     *            JNDI initial context  
     * @param queueName  
     *            name of queue  
     * @exception NamingException  
     *                if operation cannot be performed  
     * @exception JMSException  
     *                if JMS fails to initialize due to internal error  
     */  
    public void init(Context ctx, String queueName) throws NamingException,   
            JMSException {   
        qconFactory = (QueueConnectionFactory) ctx.lookup(JMS_FACTORY);   
        qcon = qconFactory.createQueueConnection();   
        qsession = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);   
        queue = (Queue) ctx.lookup(queueName);   
        qsender = qsession.createSender(queue);   
  
        msg = qsession.createTextMessage();   
        sm = qsession.createStreamMessage();   
        bm = qsession.createBytesMessage();   
        mm = qsession.createMapMessage();   
        om = qsession.createObjectMessage();   
  
        qcon.start();   
    }   
  
    /**  
     * Sends a message to a JMS queue.  
     *   
     * @param message  
     *            message to be sent  
     * @exception JMSException  
     *                if JMS fails to send message due to internal error  
     */  
    public void send(String message) throws JMSException {   
        // set TextMessage   
        msg.setText(message);   
  
        // set StreamMessage   
        sm.writeString("xmddl369");   
        sm.writeDouble(23.33);   
  
        // set BytesMessage   
        String name = "xmddl369";   
        byte[] block = name.getBytes();   
        bm.writeBytes(block);   
  
        // set MapMessage   
        mm.setString("name", "xmddl369");   
  
        // set ObjectMessage   
        UserInfo ui = new UserInfo();   
        ui.setName("xmddl369");   
        ui.setAddress("����");   
        ui.setAge(100);   
        om.setObject(ui);   
  
         qsender.send(msg);   
        // qsender.send(sm);   
        // qsender.send(bm);   
        // qsender.send(mm);   
//        qsender.send(om);   
    }   
  
    /**  
     * Closes JMS objects.  
     *   
     * @exception JMSException  
     *                if JMS fails to close objects due to internal error  
     */  
    public void close() throws JMSException {   
        qsender.close();   
        qsession.close();   
        qcon.close();   
    }   
  
    public static void main(String[] args) throws Exception {   
        InitialContext ic = getInitialContext();   
        QueueSend qs = new QueueSend();   
        qs.init(ic, QUEUE);   
        readAndSend(qs);   
        qs.close();   
    }   
  
    private static void readAndSend(QueueSend qs) throws IOException,   
            JMSException {   
        BufferedReader msgStream = new BufferedReader(new InputStreamReader(   
                System.in));   
        String line = null;   
        boolean quitNow = false;   
        do {   
            System.out.print("Enter message (/quit/ to quit): ");   
            line = msgStream.readLine();   
            if (line != null && line.trim().length() != 0) {   
                qs.send(line);   
                System.out.println("JMS Message Sent: " + line + "/n");   
                quitNow = line.equalsIgnoreCase("quit");   
            }   
        } while (!quitNow);   
  
    }   
  
    private static InitialContext getInitialContext() throws NamingException {   
        Hashtable env = new Hashtable();   
        env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);   
        env.put(Context.PROVIDER_URL, PROVIDER_URL);   
        return new InitialContext(env);   
    }   
}