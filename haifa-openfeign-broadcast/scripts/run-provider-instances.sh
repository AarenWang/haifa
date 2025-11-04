#!/usr/bin/env bash
#
# Launch multiple broadcast-provider instances backed by the assembled Spring Boot jar.
# Usage:
#   ./run-provider-instances.sh               # starts ports 8081 and 8082
#   ./run-provider-instances.sh 9001 9002     # custom ports
# Environment:
#   NACOS_SERVER   Nacos address (default localhost:8848)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
SERVER_MODULE_DIR="${MODULE_DIR}/broadcast-server"
REPO_DIR="$(cd "${MODULE_DIR}/.." && pwd)"
PORTS=("$@")

if [ ${#PORTS[@]} -eq 0 ]; then
  PORTS=(8081 8082)
fi

NACOS_SERVER="${NACOS_SERVER:-localhost:8848}"

if ! ls "${SERVER_MODULE_DIR}/target/haifa-openfeign-broadcast-server-"*.jar >/dev/null 2>&1; then
  echo "[run-provider-instances] Packaging server module (skip tests) ..."
  mvn -pl haifa-openfeign-broadcast-server -am package -DskipTests -f "${REPO_DIR}/pom.xml"
fi

JAR_PATH="$(ls "${SERVER_MODULE_DIR}/target/haifa-openfeign-broadcast-server-"*.jar | head -n 1)"
LOG_DIR="${MODULE_DIR}/logs"
mkdir -p "${LOG_DIR}"

declare -a PIDS=()

cleanup() {
  echo
  echo "[run-provider-instances] Stopping provider instances ..."
  for pid in "${PIDS[@]}"; do
    if kill -0 "${pid}" >/dev/null 2>&1; then
      kill "${pid}" >/dev/null 2>&1 || true
    fi
  done
}
trap cleanup EXIT INT TERM

echo "[run-provider-instances] Using Nacos server: ${NACOS_SERVER}"

for port in "${PORTS[@]}"; do
  LOG_FILE="${LOG_DIR}/provider-${port}.log"
  echo "[run-provider-instances] Starting provider on port ${port}; logs -> ${LOG_FILE}"
  JAVA_CMD=(java -jar "${JAR_PATH}"
    --server.port="${port}"
    --spring.cloud.nacos.discovery.server-addr="${NACOS_SERVER}")

  if [ -n "${NACOS_NAMESPACE:-}" ]; then
    JAVA_CMD+=(--spring.cloud.nacos.discovery.namespace="${NACOS_NAMESPACE}")
  fi

  "${JAVA_CMD[@]}" >"${LOG_FILE}" 2>&1 &
  PIDS+=($!)
done

echo "[run-provider-instances] All provider instances are running. Press Ctrl+C to stop."
set +e
wait
