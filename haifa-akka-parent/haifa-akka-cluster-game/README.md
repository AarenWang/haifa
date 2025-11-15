# Akka Cluster Game Sample

This module group demonstrates a minimal Akka Cluster based game server architecture that does **not** rely on
Cluster Sharding. The system is intentionally split into three Maven modules to mirror the runtime responsibilities:

- `haifa-akka-cluster-game-common` – shared protocols used by both gateway and game nodes (`PlayerCommand`, `RoomCommand`).
- `haifa-akka-cluster-game-node` – in-memory game logic containing the `GameNodeGuardian`, player/room managers and actors.
- `haifa-akka-cluster-game-gateway` – gateway layer that routes client events to a selected game node.

## Runtime Topology

```
[客户端]
   |
   v
[Gateway 节点集群] (role=gateway)
   |
   |  根据 playerId 选择一个 game 节点（hash/取模）
   v
[Game 节点集群] (role=game)
   |
   +-- GameNodeGuardian
        |
        +-- PlayerManagerActor → PlayerActor(playerId)
        +-- RoomManagerActor   → RoomActor(roomId)
```

### Actor Responsibilities

- **GatewayGuardian** – keeps track of available game nodes and forwards `PlayerCommand` messages using a deterministic hash
  of the `playerId`.
- **GameNodeGuardian** – root actor of a game node. Spawns and wires `PlayerManagerActor` and `RoomManagerActor`.
- **PlayerManagerActor** – lazily creates `PlayerActor` instances and relays incoming commands.
- **PlayerActor** – maintains mutable player state (position, current room) and sends room related commands via the
  `RoomManagerActor`.
- **RoomManagerActor** – lazily creates `RoomActor` instances.
- **RoomActor** – tracks players inside a room and handles broadcast style messaging (e.g. chat).

## Sample Launchers

Two small launcher classes show how the pieces fit together:

- `GatewayApp` boots a clustered gateway system, waits for game nodes to appear via the receptionist, and routes a
  sample command.
- `GameNodeApp` starts a standalone game node system.

These applications are intentionally lightweight and focus on illustrating the actor wiring rather than building
production ready bootstrapping logic.

## Cluster Configuration

Both the gateway and game node modules ship with an `application.conf` that demonstrates how to enable Akka Cluster with
explicit `seed-nodes` and role assignments:

- `haifa-akka-cluster-game-node/src/main/resources/application.conf` – binds to port `25520` and registers with the
  `game` role.
- `haifa-akka-cluster-game-gateway/src/main/resources/application.conf` – binds to port `25510` and joins with the
  `gateway` role.

The configuration accepts optional overrides via the `AKKA_HOST` and `AKKA_PORT` environment variables to simplify
running multiple instances on a single machine.

## One-Command Local Demo

The `run-local-demo.sh` script located in the module root orchestrates a short end-to-end run:

```bash
./run-local-demo.sh
```

It builds the project, starts a clustered game node in the background, and then runs the gateway sample which forwards a
`PlayerCommand` to the discovered node. Logs are written to `target/demo-logs` for inspection after the script finishes.
