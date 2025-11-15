package org.wrj.haifa.akka.game.node;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Guardian actor for a game node. It boots cluster sharding for player and room entities.
 */
public final class GameNodeGuardian {

    public interface Command {
    }

    public enum Stop implements Command {
        INSTANCE
    }

    public static Behavior<Command> createBehavior() {
        return Behaviors.setup(context -> new GameNodeGuardian(context).behavior());
    }

    private final ActorContext<Command> context;
    private final RedisPlayerStateRepository repository;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private GameNodeGuardian(ActorContext<Command> context) {
        this.context = context;
        this.repository = new RedisPlayerStateRepository(context.getSystem().settings().config());
        startSharding();
    }

    private Behavior<Command> behavior() {
        return Behaviors.receive(Command.class)
                .onMessage(Stop.class, msg -> {
                    onStop();
                    return Behaviors.stopped();
                })
                .onSignal(PostStop.class, signal -> {
                    onStop();
                    return Behaviors.same();
                })
                .build();
    }

    private void startSharding() {
        ClusterSharding sharding = ClusterSharding.get(context.getSystem());
        sharding.init(Entity.of(PlayerActor.ENTITY_TYPE_KEY, entityContext ->
                PlayerActor.create(entityContext, repository)).withRole("game"));
        sharding.init(Entity.of(RoomActor.ENTITY_TYPE_KEY, RoomActor::create).withRole("game"));
        context.getLog().info("Game node guardian started cluster sharding for players and rooms");
    }

    private void onStop() {
        if (closed.compareAndSet(false, true)) {
            repository.close();
        }
    }
}
