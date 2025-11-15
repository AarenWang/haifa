package org.wrj.haifa.akka.game.gateway;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.receptionist.Receptionist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.wrj.haifa.akka.game.common.player.PlayerCommand;
import org.wrj.haifa.akka.game.node.GameNodeGuardian;

/**
 * Gateway guardian simulates the edge node that accepts client events and forwards them to the game cluster.
 */
public class GatewayGuardian {

    public interface Command {
    }

    public static final class RegisterGameNode implements Command {
        public final String nodeId;
        public final ActorRef<GameNodeGuardian.Command> ref;

        public RegisterGameNode(String nodeId, ActorRef<GameNodeGuardian.Command> ref) {
            this.nodeId = nodeId;
            this.ref = ref;
        }
    }

    public static final class UnregisterGameNode implements Command {
        public final String nodeId;

        public UnregisterGameNode(String nodeId) {
            this.nodeId = nodeId;
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

    private static final class ListingUpdated implements Command {
        private final Receptionist.Listing listing;

        private ListingUpdated(Receptionist.Listing listing) {
            this.listing = listing;
        }
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new GatewayGuardian(context).behavior());
    }

    private final ActorContext<Command> context;
    private final Map<String, ActorRef<GameNodeGuardian.Command>> nodeRegistry = new LinkedHashMap<>();
    private final List<String> nodeOrder = new ArrayList<>();
    private final ActorRef<Receptionist.Listing> receptionistAdapter;

    private GatewayGuardian(ActorContext<Command> context) {
        this.context = context;
        this.receptionistAdapter = context.messageAdapter(Receptionist.Listing.class, ListingUpdated::new);
        context.getSystem()
                .receptionist()
                .tell(Receptionist.subscribe(GameNodeGuardian.SERVICE_KEY, receptionistAdapter));
    }

    private Behavior<Command> behavior() {
        return Behaviors.receive(Command.class)
                .onMessage(RegisterGameNode.class, this::onRegisterGameNode)
                .onMessage(UnregisterGameNode.class, this::onUnregisterGameNode)
                .onMessage(ForwardPlayerCommand.class, this::onForwardPlayerCommand)
                .onMessage(ListingUpdated.class, this::onListingUpdated)
                .build();
    }

    private Behavior<Command> onRegisterGameNode(RegisterGameNode register) {
        context.getLog().info("Register game node {}", register.nodeId);
        nodeRegistry.put(register.nodeId, register.ref);
        if (!nodeOrder.contains(register.nodeId)) {
            nodeOrder.add(register.nodeId);
        }
        return Behaviors.same();
    }

    private Behavior<Command> onUnregisterGameNode(UnregisterGameNode unregister) {
        context.getLog().info("Unregister game node {}", unregister.nodeId);
        nodeRegistry.remove(unregister.nodeId);
        nodeOrder.remove(unregister.nodeId);
        return Behaviors.same();
    }

    private Behavior<Command> onForwardPlayerCommand(ForwardPlayerCommand forward) {
        Optional<ActorRef<GameNodeGuardian.Command>> ref = selectNode(forward.playerId);
        if (ref.isEmpty()) {
            context.getLog().warn("No game nodes available for player {}", forward.playerId);
            return Behaviors.same();
        }
        context.getLog().debug("Route player {} to game node", forward.playerId);
        ref.get().tell(new GameNodeGuardian.PlayerEnvelope(forward.playerId, forward.command));
        return Behaviors.same();
    }

    private Optional<ActorRef<GameNodeGuardian.Command>> selectNode(String playerId) {
        if (nodeRegistry.isEmpty()) {
            return Optional.empty();
        }
        if (nodeOrder.isEmpty()) {
            nodeOrder.addAll(nodeRegistry.keySet());
        }
        int index = Math.floorMod(playerId.hashCode(), nodeOrder.size());
        String nodeId = nodeOrder.get(index);
        ActorRef<GameNodeGuardian.Command> ref = nodeRegistry.get(nodeId);
        if (ref == null) {
            nodeOrder.remove(nodeId);
            return selectNode(playerId);
        }
        return Optional.of(ref);
    }

    private Behavior<Command> onListingUpdated(ListingUpdated update) {
        Map<String, ActorRef<GameNodeGuardian.Command>> discovered = update.listing
                .getServiceInstances(GameNodeGuardian.SERVICE_KEY)
                .stream()
                .sorted((left, right) -> left.path().toString().compareTo(right.path().toString()))
                .collect(Collectors.toMap(
                        ref -> ref.path().toString(),
                        ref -> ref,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));

        nodeRegistry.clear();
        nodeRegistry.putAll(discovered);
        nodeOrder.clear();
        nodeOrder.addAll(discovered.keySet());

        context.getLog().info("Discovered {} game nodes via receptionist", nodeOrder.size());
        return Behaviors.same();
    }

    List<String> getRegisteredNodeOrder() {
        return Collections.unmodifiableList(nodeOrder);
    }
}
