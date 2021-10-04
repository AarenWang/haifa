package org.wrj.haifa.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class Main {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("defaultSystem");
        ActorRef demoActorRef = system.actorOf(Props.create(DemoActor.class),"DemoActor");
        ActorRef askDemoActorRef = system.actorOf(Props.create(AskDemoActor.class),"AskDemoActor");

        demoActorRef.tell("Hi Greeting from DemoActor",askDemoActorRef);
        askDemoActorRef.tell("Hi Greeting from askDemoActorRef",demoActorRef);


    }
}
