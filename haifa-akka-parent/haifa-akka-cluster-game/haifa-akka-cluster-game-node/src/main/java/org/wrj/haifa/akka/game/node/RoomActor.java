package org.wrj.haifa.akka.game.node;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityContext;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.wrj.haifa.akka.game.common.player.PlayerCommand;
import org.wrj.haifa.akka.game.common.room.RoomCommand;

/**
 * Sharded room entity that maintains the list of occupants and notifies players
 * of state changes through cluster sharding.
 */
public final class RoomActor {

    public static final EntityTypeKey<RoomCommand> ENTITY_TYPE_KEY =
            EntityTypeKey.create(RoomCommand.class, "RoomActor");

    public static Behavior<RoomCommand> create(EntityContext<RoomCommand> entityContext) {
        return Behaviors.setup(context -> new RoomActor(context, entityContext).behavior());
    }

    private final ActorContext<RoomCommand> context;
    private final ClusterSharding sharding;
    private final String roomId;

    private final Set<String> occupants = new HashSet<>();

    private RoomActor(ActorContext<RoomCommand> context, EntityContext<RoomCommand> entityContext) {
        this.context = context;
        this.sharding = ClusterSharding.get(context.getSystem());
        this.roomId = entityContext.getEntityId();
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
        if (occupants.add(join.playerId)) {
            context.getLog().info("Player {} join room {}", join.playerId, roomId);
            sendBroadcast(join.playerId, "joined the room");
            sendSnapshotToAll();
        } else {
            context.getLog().debug("Player {} already present in room {}", join.playerId, roomId);
        }
        return Behaviors.same();
    }

    private Behavior<RoomCommand> onPlayerLeave(RoomCommand.PlayerLeave leave) {
        if (occupants.remove(leave.playerId)) {
            context.getLog().info("Player {} leave room {}", leave.playerId, roomId);
            sendBroadcast(leave.playerId, "left the room");
            sendSnapshotToAll();
        } else {
            context.getLog().debug("Player {} not present in room {}", leave.playerId, roomId);
        }
        return Behaviors.same();
    }

    private Behavior<RoomCommand> onChat(RoomCommand.Chat chat) {
        if (!occupants.contains(chat.playerId)) {
            context.getLog().warn("Player {} attempted to chat in room {} without joining", chat.playerId, roomId);
            return Behaviors.same();
        }
        context.getLog().info("Player {} say in room {}: {}", chat.playerId, roomId, chat.message);
        sendBroadcast(chat.playerId, chat.message);
        return Behaviors.same();
    }

    private Behavior<RoomCommand> onBroadcast(RoomCommand.Broadcast broadcast) {
        sendBroadcast(broadcast.fromPlayerId, broadcast.message);
        return Behaviors.same();
    }

    private void sendBroadcast(String fromPlayerId, String message) {
        List<String> targets = List.copyOf(occupants);
        for (String playerId : targets) {
            EntityRef<PlayerCommand> playerRef = sharding.entityRefFor(PlayerActor.ENTITY_TYPE_KEY, playerId);
            playerRef.tell(new PlayerCommand.RoomBroadcast(roomId, fromPlayerId, message));
        }
    }

    private void sendSnapshotToAll() {
        if (occupants.isEmpty()) {
            return;
        }
        List<String> snapshot = List.copyOf(occupants);
        for (String playerId : snapshot) {
            EntityRef<PlayerCommand> playerRef = sharding.entityRefFor(PlayerActor.ENTITY_TYPE_KEY, playerId);
            playerRef.tell(new PlayerCommand.RoomSnapshot(roomId, snapshot));
        }
    }
}
