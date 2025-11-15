#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
LOG_DIR="$PROJECT_ROOT/target/demo-logs"

REDIS_VERSION="7.2.4"
REDIS_BASE_DIR="$PROJECT_ROOT/target/redis"
REDIS_SRC_DIR="$REDIS_BASE_DIR/redis-$REDIS_VERSION"
REDIS_TAR="$REDIS_BASE_DIR/redis-$REDIS_VERSION.tar.gz"
REDIS_SERVER_BIN="$REDIS_SRC_DIR/src/redis-server"
REDIS_CLI_BIN="$REDIS_SRC_DIR/src/redis-cli"
REDIS_PID_FILE="$LOG_DIR/redis.pid"

SEED_0="akka://GameCluster@127.0.0.1:25520"
SEED_1="akka://GameCluster@127.0.0.1:25530"

GATEWAY_PID=""
GAME_NODE_PIDS=()

log_step() {
  echo "[$(date '+%H:%M:%S')] $*"
}

build_modules() {
  log_step "Building Akka cluster game modules..."
  mvn -pl haifa-akka-cluster-game-gateway -am -DskipTests clean install | tee "$LOG_DIR/build.log"
}

install_parent_poms() {
  log_step "Installing Maven parent POMs..."
  mkdir -p "$LOG_DIR"
  mvn -f ../../pom.xml -N install | tee "$LOG_DIR/install-root-parent.log"
  mvn -f ../pom.xml -N install | tee "$LOG_DIR/install-akka-parent.log"
}

download_redis() {
  mkdir -p "$REDIS_BASE_DIR"
  if [[ -x "$REDIS_SERVER_BIN" && -x "$REDIS_CLI_BIN" ]]; then
    return
  fi

  log_step "Downloading Redis $REDIS_VERSION..."
  if [[ ! -f "$REDIS_TAR" ]]; then
    curl -fSL "https://download.redis.io/releases/redis-$REDIS_VERSION.tar.gz" -o "$REDIS_TAR"
  fi

  rm -rf "$REDIS_SRC_DIR"
  tar -xzf "$REDIS_TAR" -C "$REDIS_BASE_DIR"

  log_step "Building Redis $REDIS_VERSION (logs: $LOG_DIR/redis-build.log)..."
  (
    cd "$REDIS_SRC_DIR"
    make -j"$(nproc)" BUILD_WITH_MALLOC=libc
  ) >"$LOG_DIR/redis-build.log" 2>&1
}

start_redis() {
  mkdir -p "$LOG_DIR"
  log_step "Starting Redis server (logs: $LOG_DIR/redis.log)..."
  "$REDIS_SERVER_BIN" --save "" --appendonly no >"$LOG_DIR/redis.log" 2>&1 &
  local pid=$!
  echo "$pid" >"$REDIS_PID_FILE"

  for _ in {1..20}; do
    if ! kill -0 "$pid" >/dev/null 2>&1; then
      log_step "Redis process exited unexpectedly"
      exit 1
    fi
    if "$REDIS_CLI_BIN" ping >/dev/null 2>&1; then
      log_step "Redis is ready (pid: $pid)"
      return
    fi
    sleep 0.5
  done

  log_step "Redis did not become ready in time"
  exit 1
}

start_game_node() {
  local port="$1"
  local name="$2"
  local log_file="$LOG_DIR/game-node-$name.log"

  log_step "Starting game node $name on port $port (logs: $log_file)..."
  (
    AKKA_PORT="$port" \
      mvn -pl haifa-akka-cluster-game-node exec:java \
        -Dexec.mainClass=org.wrj.haifa.akka.game.node.GameNodeApp \
        -Dexec.cleanupDaemonThreads=false \
        -Dakka.cluster.seed-nodes.0="$SEED_0" \
        -Dakka.cluster.seed-nodes.1="$SEED_1" \
        >"$log_file" 2>&1
  ) &
  local pid=$!
  GAME_NODE_PIDS+=("$pid")
  echo "$pid" >"$LOG_DIR/game-node-$name.pid"
}

start_gateway_simulation() {
  local log_file="$LOG_DIR/gateway.log"
  log_step "Starting gateway simulation (logs: $log_file)..."
  (
    AKKA_PORT=0 \
      mvn -pl haifa-akka-cluster-game-gateway exec:java \
        -Dexec.mainClass=org.wrj.haifa.akka.game.gateway.GatewayApp \
        -Dexec.cleanupDaemonThreads=false \
        -Dakka.cluster.seed-nodes.0="$SEED_0" \
        -Dakka.cluster.seed-nodes.1="$SEED_1" \
        >"$log_file" 2>&1
  ) &
  GATEWAY_PID=$!
  echo "$GATEWAY_PID" >"$LOG_DIR/gateway.pid"
}

stop_pid() {
  local pid="$1"
  if [[ -z "$pid" ]]; then
    return
  fi
  if kill -0 "$pid" >/dev/null 2>&1; then
    kill "$pid" >/dev/null 2>&1 || true
    wait "$pid" 2>/dev/null || true
  fi
}

cleanup() {
  log_step "Shutting down demo processes..."

  if [[ -n "$GATEWAY_PID" ]]; then
    stop_pid "$GATEWAY_PID"
  fi

  for pid in "${GAME_NODE_PIDS[@]}"; do
    stop_pid "$pid"
  done

  if [[ -f "$REDIS_PID_FILE" ]]; then
    local redis_pid
    redis_pid="$(cat "$REDIS_PID_FILE")"
    stop_pid "$redis_pid"
    rm -f "$REDIS_PID_FILE"
  fi
}

trap cleanup EXIT

cd "$PROJECT_ROOT"

mkdir -p "$LOG_DIR"
install_parent_poms
build_modules

mkdir -p "$LOG_DIR" "$REDIS_BASE_DIR"

#download_redis
#start_redis

start_game_node 25520 seed-a
#sleep 3
start_game_node 25530 seed-b
sleep 5

start_gateway_simulation

if [[ -n "$GATEWAY_PID" ]]; then
  wait "$GATEWAY_PID" || true
fi

log_step "Gateway simulation finished. Inspect logs under $LOG_DIR for details."
