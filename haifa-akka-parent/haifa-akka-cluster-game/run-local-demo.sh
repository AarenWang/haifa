#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
LOG_DIR="$PROJECT_ROOT/target/demo-logs"

mkdir -p "$LOG_DIR"

build_modules() {
  mvn -pl haifa-akka-cluster-game-gateway -am -DskipTests package | tee "$LOG_DIR/build.log"
}

start_game_node() {
  mvn -pl haifa-akka-cluster-game-node exec:java \
    -Dexec.mainClass=org.wrj.haifa.akka.game.node.GameNodeApp \
    -Dexec.cleanupDaemonThreads=false \
    >"$LOG_DIR/game-node.log" 2>&1 &
  GAME_NODE_PID=$!
  echo "$GAME_NODE_PID" >"$LOG_DIR/game-node.pid"
}

run_gateway_demo() {
  mvn -pl haifa-akka-cluster-game-gateway exec:java \
    -Dexec.mainClass=org.wrj.haifa.akka.game.gateway.GatewayApp \
    -Dexec.cleanupDaemonThreads=false \
    >"$LOG_DIR/gateway.log" 2>&1 || true
}

cleanup() {
  if [[ -f "$LOG_DIR/game-node.pid" ]]; then
    local pid
    pid="$(cat "$LOG_DIR/game-node.pid")"
    if kill -0 "$pid" >/dev/null 2>&1; then
      kill "$pid" >/dev/null 2>&1 || true
      wait "$pid" 2>/dev/null || true
    fi
    rm -f "$LOG_DIR/game-node.pid"
  fi
}

trap cleanup EXIT

cd "$PROJECT_ROOT"

echo "[1/3] Building Akka cluster game modules..."
build_modules

echo "[2/3] Starting game node (logs: $LOG_DIR/game-node.log)..."
start_game_node
sleep 5

echo "[3/3] Running gateway demo (logs: $LOG_DIR/gateway.log)..."
run_gateway_demo

echo "Demo complete. Inspect logs under $LOG_DIR for details."
