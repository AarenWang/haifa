# Akka Cluster Game Sample

This module group demonstrates an Akka Cluster based game server architecture that now relies on **Cluster Sharding** to
distribute player and room entities across the game nodes. Player state is durably persisted inside Redis so that
entities can be seamlessly re-created after node failures or restarts.

The system is intentionally split into three Maven modules that mirror the runtime responsibilities:

- `haifa-akka-cluster-game-common` – shared protocols (`PlayerCommand`, `RoomCommand`).
- `haifa-akka-cluster-game-node` – hosts the sharded entities and their Redis-backed state management.
- `haifa-akka-cluster-game-gateway` – simulates clients and forwards commands directly to the sharded players.

## Runtime Topology

```
[客户端]
   |
   v
[Gateway 节点集群] (role=gateway)
   |
   |  Cluster Sharding proxy forwards消息到目标player entity
   v
[Game 节点集群] (role=game)
   |
   +-- Player Shards  (Redis持久化状态)
   +-- Room Shards
```

### Entity Responsibilities

- **PlayerActor (Entity)** – handles `PlayerCommand` messages, persists snapshots to Redis, and forwards room related
  commands via the sharded room entities.
- **RoomActor (Entity)** – maintains the set of players within a room and logs broadcast style messages.
- **GameNodeGuardian** – boots the sharded player/room entities on game nodes and wires the Redis repository.
- **GatewayGuardian** – obtains an entity reference for the target player via Cluster Sharding and sends commands.

## Redis Persistence

Player state snapshots are written to Redis under the key prefix `game:player:` (configurable via
`game.redis.player-key-prefix`). A TTL of seven days is applied by default (`game.redis.player-key-ttl`). When a player
entity starts it restores its latest snapshot from Redis, meaning reconnecting clients continue with the previously
persisted position and room membership even if their hosting node was restarted.

Configure Redis connectivity through environment variables if required:

```
export GAME_REDIS_HOST=192.168.1.10
export GAME_REDIS_PORT=6380
export GAME_REDIS_PLAYER_PREFIX=my-game:player:
```

Make sure a Redis instance is available before launching the sample applications.

## Sample Launchers

Two small launcher classes show how the pieces fit together:

- `GatewayApp` boots a clustered gateway system, sends a login, and forwards a few commands to a sharded player.
- `GameNodeApp` starts a standalone game node system hosting the sharded entities.

## Cluster Configuration

Both the gateway and game node modules ship with an `application.conf` that enables Akka Cluster with explicit
`seed-nodes`, role assignments, and cluster sharding defaults. Override the bind host/port via the `AKKA_HOST` and
`AKKA_PORT` environment variables to run multiple instances on a single machine.

### Booting the seed nodes locally

The bundled configuration expects two seed node addresses:

```
akka://GameCluster@127.0.0.1:25520
akka://GameCluster@127.0.0.1:25510
```

To start both seeds on the same workstation:

1. **First game node (port `25520`)** – uses the defaults from `application.conf`:

   ```bash
   cd haifa-akka-parent/haifa-akka-cluster-game
   mvn -pl haifa-akka-cluster-game-node exec:java \
     -Dexec.mainClass=org.wrj.haifa.akka.game.node.GameNodeApp \
     -Dexec.cleanupDaemonThreads=false
   ```

2. **Second game node (port `25510`)** – override the port before launching a second JVM:

   ```bash
   cd haifa-akka-parent/haifa-akka-cluster-game
   AKKA_PORT=25510 mvn -pl haifa-akka-cluster-game-node exec:java \
     -Dexec.mainClass=org.wrj.haifa.akka.game.node.GameNodeApp \
     -Dexec.cleanupDaemonThreads=false
   ```

   You can optionally set `AKKA_HOST=<your-ip>` if the nodes need to be reachable from another machine.

When both processes start they will form the cluster and other nodes (including the gateway sample) will automatically
discover them via the shared `seed-nodes` list.

## One-Command Local Demo

The `run-local-demo.sh` script located in the module root orchestrates a short end-to-end run:

```bash
./run-local-demo.sh
```

It builds the project, starts a clustered game node in the background (expect a running Redis instance on
`localhost:6379`), and then runs the gateway sample which forwards several `PlayerCommand` messages to the discovered
entity. Logs are written to `target/demo-logs` for inspection after the script finishes.
