package org.wrj.haifa.springsecurity.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/ws/test")
@Component
public class WebsocketEndpoint implements InitializingBean {


    private Logger logger = LoggerFactory.getLogger(WebsocketEndpoint.class);

    @OnOpen
    public  void onOpen(Session session) {
        logger.info("=================onOpen=======================");
        session.getAsyncRemote().sendText("Success");
    }

    @OnClose
    public void onClose(Session session) {
        logger.info("=================onClose=======================");


    }

    @OnMessage
    public void onMessage(String message, Session session) {

        logger.info("================= onMessage   =======================");
        logger.info("message from {} , message=【 {} 】",session.getId(),message);



    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("afterPropertiesSet");
    }
}
