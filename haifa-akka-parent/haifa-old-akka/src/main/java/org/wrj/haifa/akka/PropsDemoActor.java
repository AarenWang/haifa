package org.wrj.haifa.akka;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;

public class PropsDemoActor  extends UntypedActor {


    @Override
    public void onReceive(Object msg) throws Exception {

    }

    public static Props createProps() {
        //实现Creator接口并传入Props.create方法
        return Props.create(new Creator<PropsDemoActor>( ) {
            @Override
            public PropsDemoActor create() throws
                    Exception {    //创建Actor
                return new PropsDemoActor();
            }
        });
    }
}
