package org.wrj.haifa.akka.game.node;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityContext;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;

import java.util.Optional;

import org.wrj.haifa.akka.game.common.player.PlayerCommand;
import org.wrj.haifa.akka.game.common.room.RoomCommand;

/**
 * Sharded player entity that keeps track of player state and forwards
 * room-related commands to the room shards. Snapshots are persisted to Redis so
 * that the player can be recreated on a different node.
 */
public final class PlayerActor {

    public static final EntityTypeKey<PlayerCommand> ENTITY_TYPE_KEY =
            EntityTypeKey.create(PlayerCommand.class, "PlayerActor");

    public static Behavior<PlayerCommand> create(EntityContext<PlayerCommand> entityContext,
                                                 RedisPlayerStateRepository repository) {
        return Behaviors.setup(ctx -> new PlayerActor(ctx, entityContext, repository).behavior());
    }

    private final ActorContext<PlayerCommand> context;
    private final RedisPlayerStateRepository repository;
    private final ClusterSharding sharding;
    private final String playerId;

    private PlayerState state;

    private PlayerActor(ActorContext<PlayerCommand> context,
                        EntityContext<PlayerCommand> entityContext,
                        RedisPlayerStateRepository repository) {
        this.context = context;
        this.repository = repository;
        this.sharding = ClusterSharding.get(context.getSystem());
        this.playerId = entityContext.getEntityId();
        this.state = repository.load(playerId).orElseGet(() -> PlayerState.empty(playerId));
        state.getCurrentRoomId().ifPresent(roomId -> {
            context.getLog().info("Restoring player {} into room {}", playerId, roomId);
            roomEntity(roomId).tell(new RoomCommand.PlayerJoin(playerId));
        });
    }

    private Behavior<PlayerCommand> behavior() {
        return Behaviors.receive(PlayerCommand.class)
                .onMessage(PlayerCommand.Login.class, this::onLogin)
                .onMessage(PlayerCommand.MoveTo.class, this::onMoveTo)
                .onMessage(PlayerCommand.JoinRoom.class, this::onJoinRoom)
                .onMessage(PlayerCommand.LeaveRoom.class, this::onLeaveRoom)
                .onMessage(PlayerCommand.ChatInRoom.class, this::onChatInRoom)
                .onMessage(PlayerCommand.RoomSnapshot.class, this::onRoomSnapshot)
                .onMessage(PlayerCommand.RoomBroadcast.class, this::onRoomBroadcast)
                .build();
    }

    private Behavior<PlayerCommand> onLogin(PlayerCommand.Login login) {
        context.getLog().info("Player {} login with session {}", playerId, login.sessionId);
        state = state.withSession(login.sessionId);
        persistState();
        login.replyTo.tell(PlayerCommand.Ack.INSTANCE);
        return Behaviors.same();
    }

    private Behavior<PlayerCommand> onMoveTo(PlayerCommand.MoveTo move) {
        state = state.withPosition(move.x, move.y);
        persistState();
        context.getLog().debug("Player {} move to ({}, {})", playerId, state.getX(), state.getY());
        return Behaviors.same();
    }

    private Behavior<PlayerCommand> onJoinRoom(PlayerCommand.JoinRoom joinRoom) {
        String targetRoom = joinRoom.roomId;
        if (targetRoom == null || targetRoom.isBlank()) {
            context.getLog().warn("Player {} attempted to join an invalid room id", playerId);
            return Behaviors.same();
        }

        Optional<String> currentRoom = state.getCurrentRoomId();
        if (currentRoom.filter(targetRoom::equals).isPresent()) {
            context.getLog().debug("Player {} already in room {}", playerId, targetRoom);
            return Behaviors.same();
        }

        currentRoom.ifPresent(room -> {
            context.getLog().info("Player {} leaving room {} before joining {}", playerId, room, targetRoom);
            roomEntity(room).tell(new RoomCommand.PlayerLeave(playerId));
        });

        context.getLog().info("Player {} joining room {}", playerId, targetRoom);
        roomEntity(targetRoom).tell(new RoomCommand.PlayerJoin(playerId));
        state = state.withCurrentRoom(targetRoom);
        persistState();
        return Behaviors.same();
    }

    private Behavior<PlayerCommand> onLeaveRoom(PlayerCommand.LeaveRoom leaveRoom) {
        Optional<String> currentRoom = state.getCurrentRoomId();
        if (currentRoom.isPresent() && currentRoom.get().equals(leaveRoom.roomId)) {
            context.getLog().info("Player {} leaving room {}", playerId, leaveRoom.roomId);
            roomEntity(leaveRoom.roomId).tell(new RoomCommand.PlayerLeave(playerId));
            state = state.withCurrentRoom(null);
            persistState();
        } else {
            context.getLog().debug("Player {} ignored leave for non-joined room {}", playerId, leaveRoom.roomId);
        }
        return Behaviors.same();
    }

    private Behavior<PlayerCommand> onChatInRoom(PlayerCommand.ChatInRoom chat) {
        Optional<String> currentRoom = state.getCurrentRoomId();
        if (currentRoom.isEmpty()) {
            context.getLog().warn("Player {} chat ignored because not in any room", playerId);
            return Behaviors.same();
        }
        if (!currentRoom.get().equals(chat.roomId)) {
            context.getLog().warn("Player {} chat ignored because not in room {} (currently in {})", playerId, chat.roomId, currentRoom.get());
            return Behaviors.same();
        }
        roomEntity(chat.roomId).tell(new RoomCommand.Chat(playerId, chat.message));
        return Behaviors.same();
    }

    private Behavior<PlayerCommand> onRoomSnapshot(PlayerCommand.RoomSnapshot snapshot) {
        context.getLog().info("Player {} sees {} occupants in room {}", playerId, snapshot.occupantIds.size(), snapshot.roomId);
        return Behaviors.same();
    }

    private Behavior<PlayerCommand> onRoomBroadcast(PlayerCommand.RoomBroadcast broadcast) {
        context.getLog().info(
                "Player {} receive broadcast in room {} from {}: {}",
                playerId,
                broadcast.roomId,
                broadcast.fromPlayerId,
                broadcast.message);
        return Behaviors.same();
    }

    private EntityRef<RoomCommand> roomEntity(String roomId) {
        return sharding.entityRefFor(RoomActor.ENTITY_TYPE_KEY, roomId);
    }

    private void persistState() {
        repository.save(state);
    }
}
