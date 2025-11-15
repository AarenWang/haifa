package org.wrj.haifa.akka.game.gateway;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.actor.typed.javadsl.Behaviors;

import org.wrj.haifa.akka.game.common.player.PlayerCommand;
import org.wrj.haifa.akka.game.node.PlayerActor;

/**
 * Gateway guardian simulates the edge node that accepts client events and forwards them to sharded player entities.
 */
public final class GatewayGuardian {

    public interface Command {
    }

    public static final class PlayerLogin implements Command {
        public final String playerId;
        public final String sessionId;

        public PlayerLogin(String playerId, String sessionId) {
            this.playerId = playerId;
            this.sessionId = sessionId;
        }
    }

    public static final class ForwardPlayerCommand implements Command {
        public final String playerId;
        public final PlayerCommand command;

        public ForwardPlayerCommand(String playerId, PlayerCommand command) {
            this.playerId = playerId;
            this.command = command;
        }
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new GatewayGuardian(context).behavior());
    }

    private final ActorContext<Command> context;
    private final ClusterSharding sharding;

    private GatewayGuardian(ActorContext<Command> context) {
        this.context = context;
        this.sharding = ClusterSharding.get(context.getSystem());
        // Ensure shard proxy is initialized on gateway nodes so entityRefFor() works
        // The .withRole("game") means the actual entities are hosted on nodes with role 'game'
        // and this call will register a proxy on the gateway system.
        sharding.init(Entity.of(PlayerActor.ENTITY_TYPE_KEY, entityContext -> Behaviors.empty()).withRole("game"));
    }

    private Behavior<Command> behavior() {
        return Behaviors.receive(Command.class)
                .onMessage(PlayerLogin.class, this::onPlayerLogin)
                .onMessage(ForwardPlayerCommand.class, this::onForwardPlayerCommand)
                .build();
    }

    private Behavior<Command> onPlayerLogin(PlayerLogin login) {
        EntityRef<PlayerCommand> entityRef = sharding.entityRefFor(PlayerActor.ENTITY_TYPE_KEY, login.playerId);
        ActorRef<PlayerCommand.Ack> ackRef = context.spawnAnonymous(Behaviors.receiveMessage(msg -> {
            context.getLog().info("Player {} login acknowledged", login.playerId);
            return Behaviors.stopped();
        }));
        entityRef.tell(new PlayerCommand.Login(login.sessionId, ackRef));
        context.getLog().info("Forward login for player {}", login.playerId);
        return Behaviors.same();
    }

    private Behavior<Command> onForwardPlayerCommand(ForwardPlayerCommand forward) {
        EntityRef<PlayerCommand> entityRef = sharding.entityRefFor(PlayerActor.ENTITY_TYPE_KEY, forward.playerId);
        entityRef.tell(forward.command);
        context.getLog().debug("Forward {} to sharded player {}", forward.command.getClass().getSimpleName(), forward.playerId);
        return Behaviors.same();
    }
}
