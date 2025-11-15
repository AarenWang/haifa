package org.wrj.haifa.akka.game.common.player;

import akka.actor.typed.ActorRef;

import java.util.List;

/**
 * Common protocol for player level commands exchanged between the gateway and game nodes.
 */
public interface PlayerCommand {

    /** Login request triggered by the client once a connection is established. */
    final class Login implements PlayerCommand {
        public final String sessionId;
        public final ActorRef<Ack> replyTo;

        public Login(String sessionId, ActorRef<Ack> replyTo) {
            this.sessionId = sessionId;
            this.replyTo = replyTo;
        }
    }

    /** Move the player to the provided coordinate. */
    final class MoveTo implements PlayerCommand {
        public final int x;
        public final int y;

        public MoveTo(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /** Request to join a specific room. */
    final class JoinRoom implements PlayerCommand {
        public final String roomId;

        public JoinRoom(String roomId) {
            this.roomId = roomId;
        }
    }

    /** Request to leave a specific room. */
    final class LeaveRoom implements PlayerCommand {
        public final String roomId;

        public LeaveRoom(String roomId) {
            this.roomId = roomId;
        }
    }

    /**
     * Message triggered by the client to chat in a room. The PlayerActor will verify the state before forwarding to
     * the room actor.
     */
    final class ChatInRoom implements PlayerCommand {
        public final String roomId;
        public final String message;

        public ChatInRoom(String roomId, String message) {
            this.roomId = roomId;
            this.message = message;
        }
    }

    /**
     * Minimal acknowledgement protocol that the gateway can use to confirm state changing commands.
     */
    final class Ack {
        public static final Ack INSTANCE = new Ack();

        private Ack() {
        }
    }

    /**
     * Snapshot of the current room occupants that helps a client maintain UI state.
     */
    final class RoomSnapshot implements PlayerCommand {
        public final String roomId;
        public final List<String> occupantIds;

        public RoomSnapshot(String roomId, List<String> occupantIds) {
            this.roomId = roomId;
            this.occupantIds = List.copyOf(occupantIds);
        }
    }

    /**
     * Notification emitted by a room and delivered to the player so the client can react to the broadcast.
     */
    final class RoomBroadcast implements PlayerCommand {
        public final String roomId;
        public final String fromPlayerId;
        public final String message;

        public RoomBroadcast(String roomId, String fromPlayerId, String message) {
            this.roomId = roomId;
            this.fromPlayerId = fromPlayerId;
            this.message = message;
        }
    }
}
