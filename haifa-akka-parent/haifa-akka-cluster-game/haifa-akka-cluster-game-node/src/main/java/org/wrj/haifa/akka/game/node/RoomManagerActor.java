package org.wrj.haifa.akka.game.node;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;

import java.util.HashMap;
import java.util.Map;

import org.wrj.haifa.akka.game.common.room.RoomCommand;

/**
 * Actor responsible for on-demand creation and routing to {@link RoomActor} instances.
 */
public class RoomManagerActor {

    public interface Command {
    }

    public static final class RouteToRoom implements Command {
        public final String roomId;
        public final RoomCommand command;

        public RouteToRoom(String roomId, RoomCommand command) {
            this.roomId = roomId;
            this.command = command;
        }
    }

    public static Behavior<Command> createBehavior() {
        return Behaviors.setup(context -> new RoomManagerActor(context).behavior());
    }

    private final ActorContext<Command> context;
    private final Map<String, ActorRef<RoomCommand>> rooms = new HashMap<>();

    private RoomManagerActor(ActorContext<Command> context) {
        this.context = context;
    }

    private Behavior<Command> behavior() {
        return Behaviors.receive(Command.class)
                .onMessage(RouteToRoom.class, this::onRouteToRoom)
                .build();
    }

    private Behavior<Command> onRouteToRoom(RouteToRoom routeToRoom) {
        ActorRef<RoomCommand> roomRef = rooms.computeIfAbsent(routeToRoom.roomId, id -> {
            context.getLog().debug("Spawning RoomActor for {}", id);
            return context.spawn(RoomActor.create(id), "room-" + id);
        });
        roomRef.tell(routeToRoom.command);
        return Behaviors.same();
    }
}
