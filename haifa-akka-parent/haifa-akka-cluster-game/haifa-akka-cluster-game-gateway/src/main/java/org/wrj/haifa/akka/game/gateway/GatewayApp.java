package org.wrj.haifa.akka.game.gateway;

import akka.actor.typed.ActorSystem;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.wrj.haifa.akka.game.common.player.PlayerCommand;

/**
 * Demonstrates how the gateway and a single game node interact.
 */
public final class GatewayApp {

    private GatewayApp() {
    }

    public static void main(String[] args) throws InterruptedException {
        Config config = ConfigFactory.load();

        ActorSystem<GatewayGuardian.Command> gateway = ActorSystem.create(
                GatewayGuardian.create(),
                "GameCluster",
                config);

        Thread.sleep(2000L);

        gateway.tell(new GatewayGuardian.ForwardPlayerCommand("player-42", new PlayerCommand.MoveTo(10, 20)));

        Thread.sleep(2000L);

        gateway.terminate();
        gateway.getWhenTerminated().toCompletableFuture().join();
    }
}
