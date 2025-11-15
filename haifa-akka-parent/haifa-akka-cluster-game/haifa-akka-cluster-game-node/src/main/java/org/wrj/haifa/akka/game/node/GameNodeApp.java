package org.wrj.haifa.akka.game.node;

import akka.actor.typed.ActorSystem;
import akka.management.javadsl.AkkaManagement;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Sample application that boots a single game node actor system.
 */
public final class GameNodeApp {

    private GameNodeApp() {
    }

    public static void main(String[] args) {
        Config config = ConfigFactory.load();

        ActorSystem<GameNodeGuardian.Command> system = ActorSystem.create(
                GameNodeGuardian.createBehavior(),
                "GameCluster",
                config);


        try{
            // 启动管理服务
            AkkaManagement.get(system).start();
        } catch (Exception  e) {
            // ignore 
            e.printStackTrace();

        }
        
        system.getWhenTerminated().toCompletableFuture().join();
    }
}
