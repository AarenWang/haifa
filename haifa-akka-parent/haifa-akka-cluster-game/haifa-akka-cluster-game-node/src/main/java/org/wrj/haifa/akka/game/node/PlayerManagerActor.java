package org.wrj.haifa.akka.game.node;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;

import java.util.HashMap;
import java.util.Map;

import org.wrj.haifa.akka.game.common.player.PlayerCommand;

/**
 * Actor responsible for maintaining the player actor instances on a game node.
 */
public class PlayerManagerActor {

    public interface Command {
    }

    public static final class RouteToPlayer implements Command {
        public final String playerId;
        public final PlayerCommand command;
        public final ActorRef<RoomManagerActor.Command> roomManager;

        public RouteToPlayer(String playerId,
                             PlayerCommand command,
                             ActorRef<RoomManagerActor.Command> roomManager) {
            this.playerId = playerId;
            this.command = command;
            this.roomManager = roomManager;
        }
    }

    public static Behavior<Command> createBehavior() {
        return Behaviors.setup(context -> new PlayerManagerActor(context).behavior());
    }

    private final ActorContext<Command> context;
    private final Map<String, ActorRef<PlayerCommand>> players = new HashMap<>();

    private PlayerManagerActor(ActorContext<Command> context) {
        this.context = context;
    }

    private Behavior<Command> behavior() {
        return Behaviors.receive(Command.class)
                .onMessage(RouteToPlayer.class, this::onRouteToPlayer)
                .build();
    }

    private Behavior<Command> onRouteToPlayer(RouteToPlayer routeToPlayer) {
        ActorRef<PlayerCommand> playerRef = players.computeIfAbsent(routeToPlayer.playerId, id -> {
            context.getLog().debug("Spawning PlayerActor for {}", id);
            return context.spawn(PlayerActor.create(id, routeToPlayer.roomManager), "player-" + id);
        });
        playerRef.tell(routeToPlayer.command);
        return Behaviors.same();
    }
}
