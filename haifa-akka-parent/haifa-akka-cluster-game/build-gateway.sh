#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
# Build gateway and its dependencies quickly (skip tests)
mvn -pl haifa-akka-cluster-game-gateway -am clean package -DskipTests

echo "Gateway build finished"
