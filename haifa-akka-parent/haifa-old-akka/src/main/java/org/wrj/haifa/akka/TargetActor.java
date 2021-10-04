package org.wrj.haifa.akka;

import akka.actor.UntypedActor;

public class TargetActor extends UntypedActor {


    @Override
    public void onReceive(Object msg) throws Exception {
        System.out.println("TargetActor receive: " + msg + "，sender=" + getSender());
    }
}
