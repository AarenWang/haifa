package org.wrj.haifa.akka.game.node;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;

import org.wrj.haifa.akka.game.common.player.PlayerCommand;
import org.wrj.haifa.akka.game.common.room.RoomCommand;

/**
 * Actor that encapsulates the player state on a game node.
 */
public class PlayerActor {

    public static Behavior<PlayerCommand> create(String playerId, ActorRef<RoomManagerActor.Command> roomManager) {
        return Behaviors.setup(ctx -> new PlayerActor(ctx, playerId, roomManager).behavior());
    }

    private final ActorContext<PlayerCommand> context;
    private final String playerId;
    private final ActorRef<RoomManagerActor.Command> roomManager;

    private int x = 0;
    private int y = 0;
    private String currentRoomId;

    private PlayerActor(ActorContext<PlayerCommand> context,
                        String playerId,
                        ActorRef<RoomManagerActor.Command> roomManager) {
        this.context = context;
        this.playerId = playerId;
        this.roomManager = roomManager;
    }

    private Behavior<PlayerCommand> behavior() {
        return Behaviors.receive(PlayerCommand.class)
                .onMessage(PlayerCommand.Login.class, this::onLogin)
                .onMessage(PlayerCommand.MoveTo.class, this::onMoveTo)
                .onMessage(PlayerCommand.JoinRoom.class, this::onJoinRoom)
                .onMessage(PlayerCommand.LeaveRoom.class, this::onLeaveRoom)
                .onMessage(PlayerCommand.ChatInRoom.class, this::onChatInRoom)
                .build();
    }

    private Behavior<PlayerCommand> onLogin(PlayerCommand.Login login) {
        context.getLog().info("Player {} login with session {}", playerId, login.sessionId);
        login.replyTo.tell(PlayerCommand.Ack.INSTANCE);
        return Behaviors.same();
    }

    private Behavior<PlayerCommand> onMoveTo(PlayerCommand.MoveTo move) {
        this.x = move.x;
        this.y = move.y;
        context.getLog().debug("Player {} move to ({}, {})", playerId, x, y);
        return Behaviors.same();
    }

    private Behavior<PlayerCommand> onJoinRoom(PlayerCommand.JoinRoom joinRoom) {
        this.currentRoomId = joinRoom.roomId;
        roomManager.tell(new RoomManagerActor.RouteToRoom(
                joinRoom.roomId,
                new RoomCommand.PlayerJoin(playerId, context.getSelf())
        ));
        return Behaviors.same();
    }

    private Behavior<PlayerCommand> onLeaveRoom(PlayerCommand.LeaveRoom leaveRoom) {
        if (currentRoomId != null && currentRoomId.equals(leaveRoom.roomId)) {
            roomManager.tell(new RoomManagerActor.RouteToRoom(
                    leaveRoom.roomId,
                    new RoomCommand.PlayerLeave(playerId)
            ));
            currentRoomId = null;
        }
        return Behaviors.same();
    }

    private Behavior<PlayerCommand> onChatInRoom(PlayerCommand.ChatInRoom chat) {
        if (currentRoomId == null || !currentRoomId.equals(chat.roomId)) {
            context.getLog().warn("Player {} chat in not-joined room {}", playerId, chat.roomId);
            return Behaviors.same();
        }
        roomManager.tell(new RoomManagerActor.RouteToRoom(
                chat.roomId,
                new RoomCommand.Chat(playerId, chat.message)
        ));
        return Behaviors.same();
    }
}
