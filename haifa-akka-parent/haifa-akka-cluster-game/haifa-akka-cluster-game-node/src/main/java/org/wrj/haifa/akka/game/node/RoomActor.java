package org.wrj.haifa.akka.game.node;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;

import java.util.HashMap;
import java.util.Map;

import org.wrj.haifa.akka.game.common.player.PlayerCommand;
import org.wrj.haifa.akka.game.common.room.RoomCommand;

/**
 * Actor that keeps track of in-memory room state.
 */
public class RoomActor {

    public static Behavior<RoomCommand> create(String roomId) {
        return Behaviors.setup(context -> new RoomActor(context, roomId).behavior());
    }

    private final ActorContext<RoomCommand> context;
    private final String roomId;
    private final Map<String, ActorRef<PlayerCommand>> players = new HashMap<>();

    private RoomActor(ActorContext<RoomCommand> context, String roomId) {
        this.context = context;
        this.roomId = roomId;
    }

    private Behavior<RoomCommand> behavior() {
        return Behaviors.receive(RoomCommand.class)
                .onMessage(RoomCommand.PlayerJoin.class, this::onPlayerJoin)
                .onMessage(RoomCommand.PlayerLeave.class, this::onPlayerLeave)
                .onMessage(RoomCommand.Chat.class, this::onChat)
                .onMessage(RoomCommand.Broadcast.class, this::onBroadcast)
                .build();
    }

    private Behavior<RoomCommand> onPlayerJoin(RoomCommand.PlayerJoin join) {
        context.getLog().info("Player {} join room {}", join.playerId, roomId);
        players.put(join.playerId, join.playerRef);
        broadcast(new RoomCommand.Broadcast(join.playerId, "joined the room"));
        return Behaviors.same();
    }

    private Behavior<RoomCommand> onPlayerLeave(RoomCommand.PlayerLeave leave) {
        context.getLog().info("Player {} leave room {}", leave.playerId, roomId);
        players.remove(leave.playerId);
        broadcast(new RoomCommand.Broadcast(leave.playerId, "left the room"));
        return Behaviors.same();
    }

    private Behavior<RoomCommand> onChat(RoomCommand.Chat chat) {
        context.getLog().info("Player {} say in room {}: {}", chat.playerId, roomId, chat.message);
        broadcast(new RoomCommand.Broadcast(chat.playerId, chat.message));
        return Behaviors.same();
    }

    private Behavior<RoomCommand> onBroadcast(RoomCommand.Broadcast broadcast) {
        players.forEach((player, ref) -> context.getLog().debug(
                "Send message to {}: [{}] {}", player, broadcast.fromPlayerId, broadcast.message));
        return Behaviors.same();
    }

    private void broadcast(RoomCommand.Broadcast broadcast) {
        context.getSelf().tell(broadcast);
    }
}
