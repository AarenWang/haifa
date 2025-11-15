package org.wrj.haifa.akka.game.node;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;

import org.wrj.haifa.akka.game.common.player.PlayerCommand;
import org.wrj.haifa.akka.game.common.room.RoomCommand;

/**
 * Guardian actor for a game node. It initializes the player and room managers and exposes the entry protocol used by
 * the gateway layer.
 */
public class GameNodeGuardian {

    public interface Command {
    }

    public static final ServiceKey<Command> SERVICE_KEY =
            ServiceKey.create(Command.class, "game-node-guardian-service");

    public static final class PlayerEnvelope implements Command {
        public final String playerId;
        public final PlayerCommand command;

        public PlayerEnvelope(String playerId, PlayerCommand command) {
            this.playerId = playerId;
            this.command = command;
        }
    }

    public static final class RoomEnvelope implements Command {
        public final String roomId;
        public final RoomCommand command;

        public RoomEnvelope(String roomId, RoomCommand command) {
            this.roomId = roomId;
            this.command = command;
        }
    }

    public static Behavior<Command> createBehavior() {
        return Behaviors.setup(context -> new GameNodeGuardian(context).behavior());
    }

    private final ActorContext<Command> context;
    private final ActorRef<PlayerManagerActor.Command> playerManager;
    private final ActorRef<RoomManagerActor.Command> roomManager;

    private GameNodeGuardian(ActorContext<Command> context) {
        this.context = context;
        this.roomManager = context.spawn(RoomManagerActor.createBehavior(), "room-manager");
        this.playerManager = context.spawn(PlayerManagerActor.createBehavior(), "player-manager");
        context.getSystem()
                .receptionist()
                .tell(Receptionist.register(SERVICE_KEY, context.getSelf()));
    }

    private Behavior<Command> behavior() {
        return Behaviors.receive(Command.class)
                .onMessage(PlayerEnvelope.class, this::onPlayerEnvelope)
                .onMessage(RoomEnvelope.class, this::onRoomEnvelope)
                .onSignal(PostStop.class, signal -> {
                    context.getSystem()
                            .receptionist()
                            .tell(Receptionist.deregister(SERVICE_KEY, context.getSelf()));
                    return Behaviors.same();
                })
                .build();
    }

    private Behavior<Command> onPlayerEnvelope(PlayerEnvelope envelope) {
        playerManager.tell(new PlayerManagerActor.RouteToPlayer(envelope.playerId, envelope.command, roomManager));
        return Behaviors.same();
    }

    private Behavior<Command> onRoomEnvelope(RoomEnvelope envelope) {
        roomManager.tell(new RoomManagerActor.RouteToRoom(envelope.roomId, envelope.command));
        return Behaviors.same();
    }
}
