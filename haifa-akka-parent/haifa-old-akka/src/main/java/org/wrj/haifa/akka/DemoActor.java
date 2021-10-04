package org.wrj.haifa.akka;

import akka.actor.UntypedActor;
import akka.event.Logging;import akka.event.LoggingAdapter;

public class DemoActor extends UntypedActor {
    private LoggingAdapter log =Logging.getLogger(this .getContext().system(), this);

    @Override    public void onReceive(Object msg) {
        System.out.println( "发送者是：" +getSender() + " 发送内容: "+msg.toString());
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        getSender().tell( "hello " +msg , getSelf());
    }

}