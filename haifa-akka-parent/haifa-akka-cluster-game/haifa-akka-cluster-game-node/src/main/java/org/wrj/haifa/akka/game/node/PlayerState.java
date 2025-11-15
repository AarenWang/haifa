package org.wrj.haifa.akka.game.node;

import java.util.Objects;
import java.util.Optional;

/**
 * Snapshot of a player's state that can be serialized into Redis.
 */
public final class PlayerState {

    private final String playerId;
    private final int x;
    private final int y;
    private final String currentRoomId;
    private final String sessionId;

    private PlayerState(String playerId, int x, int y, String currentRoomId, String sessionId) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.x = x;
        this.y = y;
        this.currentRoomId = currentRoomId;
        this.sessionId = sessionId;
    }

    public static PlayerState empty(String playerId) {
        return new PlayerState(playerId, 0, 0, null, null);
    }

    public static PlayerState deserialize(String playerId, String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return empty(playerId);
        }
        String[] parts = serialized.split("\\|", -1);
        try {
            int x = parts.length > 0 && !parts[0].isEmpty() ? Integer.parseInt(parts[0]) : 0;
            int y = parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1]) : 0;
            String room = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null;
            String session = parts.length > 3 && !parts[3].isEmpty() ? parts[3] : null;
            return new PlayerState(playerId, x, y, room, session);
        } catch (NumberFormatException ex) {
            return empty(playerId);
        }
    }

    public String serialize() {
        return String.format("%s|%s|%s|%s",
                x,
                y,
                Optional.ofNullable(currentRoomId).orElse(""),
                Optional.ofNullable(sessionId).orElse(""));
    }

    public PlayerState withPosition(int newX, int newY) {
        return new PlayerState(playerId, newX, newY, currentRoomId, sessionId);
    }

    public PlayerState withCurrentRoom(String roomId) {
        return new PlayerState(playerId, x, y, roomId, sessionId);
    }

    public PlayerState withSession(String newSession) {
        return new PlayerState(playerId, x, y, currentRoomId, newSession);
    }

    public String getPlayerId() {
        return playerId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Optional<String> getCurrentRoomId() {
        return Optional.ofNullable(currentRoomId);
    }

    public Optional<String> getSessionId() {
        return Optional.ofNullable(sessionId);
    }

    @Override
    public String toString() {
        return "PlayerState{" +
                "playerId='" + playerId + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", currentRoomId='" + currentRoomId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
}
