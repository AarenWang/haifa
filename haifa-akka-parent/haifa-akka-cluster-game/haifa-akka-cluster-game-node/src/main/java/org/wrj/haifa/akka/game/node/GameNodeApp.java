package org.wrj.haifa.akka.game.node;

import akka.actor.typed.ActorSystem;

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

        system.getWhenTerminated().toCompletableFuture().join();
    }
}
