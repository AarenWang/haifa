package org.wrj.haifa.akka.game.gateway;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Props;
import akka.actor.typed.javadsl.Behaviors;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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

        AtomicInteger acknowledgementCount = new AtomicInteger();
        Behavior<PlayerCommand.Ack> ackBehavior = Behaviors.setup(context ->
                Behaviors.receive(PlayerCommand.Ack.class)
                        .onMessage(PlayerCommand.Ack.class, ack -> {
                            int total = acknowledgementCount.incrementAndGet();
                            context.getLog().info("Received acknowledgement from game cluster (total: {})", total);
                            return Behaviors.same();
                        })
                        .build());

        ActorRef<PlayerCommand.Ack> ackLogger = gateway.systemActorOf(ackBehavior, "ack-logger", Props.empty());

        List<String> players = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            players.add("player-" + i);
        }

        List<String> rooms = List.of("room-alpha", "room-beta");
        Map<String, String> playerRooms = new HashMap<>();

        Thread.sleep(Duration.ofSeconds(5).toMillis());

        for (String playerId : players) {
            gateway.tell(new GatewayGuardian.ForwardPlayerCommand(
                    playerId,
                    new PlayerCommand.Login(UUID.randomUUID().toString(), ackLogger)));
            Thread.sleep(500L);
        }

        int joinCount = 0;
        int leaveCount = 0;
        for (int i = 0; i < players.size(); i++) {
            String playerId = players.get(i);
            String roomId = rooms.get(i % rooms.size());
            playerRooms.put(playerId, roomId);
            gateway.tell(new GatewayGuardian.ForwardPlayerCommand(
                    playerId,
                    new PlayerCommand.JoinRoom(roomId)));
            Thread.sleep(500L);
            joinCount++;
        }

        long demoDurationMs = Duration.ofMinutes(20).toMillis();
        long endTime = System.currentTimeMillis() + demoDurationMs;
        Random random = new Random();
        String[] chatTemplates = {
                "Hello room!",
                "How is everyone doing?",
                "Any quests available?",
                "Defending the base!",
                "Collecting resources.",
                "Need backup here!"
        };

        int chatCount = 0;
        int moveCount = 0;
        int switchCount = 0;

        while (System.currentTimeMillis() < endTime) {
            String playerId = players.get(random.nextInt(players.size()));
            int roll = random.nextInt(100);

            if (roll < 50) {
                String roomId = playerRooms.get(playerId);
                if (roomId != null) {
                    String message = chatTemplates[random.nextInt(chatTemplates.length)];
                    gateway.tell(new GatewayGuardian.ForwardPlayerCommand(
                            playerId,
                            new PlayerCommand.ChatInRoom(roomId, message)));
                    chatCount++;
                }
            } else if (roll < 80) {
                int x = random.nextInt(100);
                int y = random.nextInt(100);
                gateway.tell(new GatewayGuardian.ForwardPlayerCommand(
                        playerId,
                        new PlayerCommand.MoveTo(x, y)));
                moveCount++;
            } else {
                String currentRoom = playerRooms.get(playerId);
                String targetRoom = rooms.get(random.nextInt(rooms.size()));
                if (currentRoom == null || !currentRoom.equals(targetRoom)) {
                    if (currentRoom != null) {
                        gateway.tell(new GatewayGuardian.ForwardPlayerCommand(
                                playerId,
                                new PlayerCommand.LeaveRoom(currentRoom)));
                        Thread.sleep(200L);
                        leaveCount++;
                    }
                    playerRooms.put(playerId, targetRoom);
                    gateway.tell(new GatewayGuardian.ForwardPlayerCommand(
                            playerId,
                            new PlayerCommand.JoinRoom(targetRoom)));
                    joinCount++;
                    switchCount++;
                }
            }

            Thread.sleep(500L);
        }

        for (String playerId : players) {
            String roomId = playerRooms.get(playerId);
            if (roomId != null) {
                gateway.tell(new GatewayGuardian.ForwardPlayerCommand(
                        playerId,
                        new PlayerCommand.LeaveRoom(roomId)));
                Thread.sleep(200L);
                leaveCount++;
            }
        }

        gateway.log().info(
                "Simulation summary -> joins: {}, leaves: {}, chats: {}, moves: {}, switches: {}, acknowledgements: {}",
                joinCount,
                leaveCount,
                chatCount,
                moveCount,
                switchCount,
                acknowledgementCount.get());

        gateway.terminate();
        gateway.getWhenTerminated().toCompletableFuture().join();
    }
}
