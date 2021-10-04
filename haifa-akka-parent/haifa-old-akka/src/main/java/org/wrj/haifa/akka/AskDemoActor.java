package org.wrj.haifa.akka;


import akka.actor.UntypedActor;

public class AskDemoActor extends UntypedActor {


    @Override
    public void onReceive(Object msg) throws Exception {
        System.out.println( "发送者是：" +getSender() + " 发送内容: "+msg.toString());
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        getSender().tell( "hello " +msg , getSelf());
    }



}
