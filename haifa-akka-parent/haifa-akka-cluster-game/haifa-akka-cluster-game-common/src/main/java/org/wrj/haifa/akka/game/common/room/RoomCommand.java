package org.wrj.haifa.akka.game.common.room;

import akka.actor.typed.ActorRef;
import org.wrj.haifa.akka.game.common.player.PlayerCommand;

/**
 * Protocol used by the room subsystem within a game node.
 */
public interface RoomCommand {

    /** Command that indicates a player is joining a room. */
    final class PlayerJoin implements RoomCommand {
        public final String playerId;
        public final ActorRef<PlayerCommand> playerRef;

        public PlayerJoin(String playerId, ActorRef<PlayerCommand> playerRef) {
            this.playerId = playerId;
            this.playerRef = playerRef;
        }
    }

    /** Command that indicates a player is leaving the room. */
    final class PlayerLeave implements RoomCommand {
        public final String playerId;

        public PlayerLeave(String playerId) {
            this.playerId = playerId;
        }
    }

    /** Chat message issued by a player and routed via the PlayerActor. */
    final class Chat implements RoomCommand {
        public final String playerId;
        public final String message;

        public Chat(String playerId, String message) {
            this.playerId = playerId;
            this.message = message;
        }
    }

    /** Internal broadcast event produced by the room itself. */
    final class Broadcast implements RoomCommand {
        public final String fromPlayerId;
        public final String message;

        public Broadcast(String fromPlayerId, String message) {
            this.fromPlayerId = fromPlayerId;
            this.message = message;
        }
    }
}
