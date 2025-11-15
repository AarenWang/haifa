package org.wrj.haifa.akka.game.node;

import com.typesafe.config.Config;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Repository that persists player snapshots into Redis so state can survive node restarts.
 */
public final class RedisPlayerStateRepository implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisPlayerStateRepository.class);

    private final JedisPooled jedis;
    private final String keyPrefix;
    private final Duration ttl;

    public RedisPlayerStateRepository(Config config) {
        Config redisConfig = config.getConfig("game.redis");
        String host = redisConfig.getString("host");
        int port = redisConfig.getInt("port");
        this.keyPrefix = redisConfig.getString("player-key-prefix");
        long ttlSeconds = redisConfig.getDuration("player-key-ttl").getSeconds();
        this.ttl = ttlSeconds > 0 ? Duration.ofSeconds(ttlSeconds) : Duration.ZERO;
        this.jedis = new JedisPooled(host, port);
        LOGGER.info("Initialized RedisPlayerStateRepository with host {}:{}, prefix {}", host, port, keyPrefix);
    }

    public Optional<PlayerState> load(String playerId) {
        String value;
        try {
            value = jedis.get(key(playerId));
        } catch (JedisException ex) {
            LOGGER.error("Failed to load player {} state from Redis", playerId, ex);
            return Optional.empty();
        }
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(PlayerState.deserialize(playerId, value));
    }

    public void save(PlayerState state) {
        String key = key(state.getPlayerId());
        try {
            if (ttl.isZero()) {
                jedis.set(key, state.serialize());
            } else {
                jedis.setex(key, (int) ttl.getSeconds(), state.serialize());
            }
        } catch (JedisException ex) {
            LOGGER.error("Failed to persist player {} state to Redis", state.getPlayerId(), ex);
        }
    }

    private String key(String playerId) {
        return keyPrefix + playerId;
    }

    @Override
    public void close() {
        try {
            jedis.close();
        } catch (Exception ex) {
            LOGGER.warn("Error while closing Redis connection", ex);
        }
    }
}
