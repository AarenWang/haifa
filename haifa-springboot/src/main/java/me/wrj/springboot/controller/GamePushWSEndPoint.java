package me.wrj.springboot.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/game/bomb")
@Component
public class GamePushWSEndPoint {

    private static final Logger logger = LoggerFactory.getLogger(GamePushWSEndPoint.class);

    @OnOpen
    public  void onOpen(Session session) {
        logger.info("onOpen, sessionId: {}", session.getId(),session.getOpenSessions());
    }


    @OnClose
    public void onClose(Session session) {
        logger.info("onOpen");
    }


    @OnMessage
    public void onMessage(String message, Session session) {
        logger.info("onMessage, sessionId: {}, message: {}", session.getId(), message);
    }


}
