#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
# Build gateway and its dependencies quickly (skip tests)
# Use `install` so that downstream Maven commands (e.g. exec:java) can
# resolve the freshly built sibling modules from the local repository.
mvn -pl haifa-akka-cluster-game-gateway -am clean install -DskipTests

echo "Gateway build finished"
