# Haifa OpenFeign Broadcast Demo

This multi-module sample demonstrates a broadcast-style OpenFeign extension where a single client method call fan-outs to every discovered service instance. The setup uses Spring Cloud OpenFeign with Nacos for service discovery, alongside a small RPC provider and client that verifies end-to-end behaviour.

## What’s Included
- **`rpc`** – shared RPC contract module (`haifa-openfeign-broadcast-rpc`) containing the Feign client interface and public request/response DTOs.
- **`openfeign-extension`** – reusable library (`haifa-openfeign-broadcast-extension`) providing the broadcast-aware Feign load-balancer and infrastructure. This jar depends on the RPC module and can be published for other projects.
- **`broadcast-server`** – Spring Boot service (`BroadcastServerApplication`) that stores broadcast messages in-memory for each instance.
- **`broadcast-client`** – Spring Boot app (`BroadcastClientApplication`) that triggers broadcast calls and exposes a verification REST API.
- **Docker quick start** – `docker/docker-compose.yml` launches a standalone Nacos registry.

## Prerequisites
- JDK 17+
- Maven 3.8+
- Docker (for the registry)

## Start the Service Registry
Spin up a standalone Nacos server with Docker Compose.
```bash
cd haifa-openfeign-broadcast/docker
docker compose up -d
# Nacos console: http://localhost:8848/nacos (username/password: nacos/nacos)
```

## Run Provider Instances
Start at least two instances so the broadcast call has multiple targets. A helper script is provided under `scripts/`:
```bash
cd haifa-openfeign-broadcast/scripts
chmod +x run-provider-instances.sh        # first run only
./run-provider-instances.sh               # defaults to ports 8081 & 8082
# custom ports / Nacos address
NACOS_SERVER=127.0.0.1:8848 ./run-provider-instances.sh 9001 9002
```
The script builds the server module (if needed), launches one JVM per port, and writes logs to `haifa-openfeign-broadcast/logs/provider-<port>.log`. Use `Ctrl+C` to stop all instances together.

Prefer manual control? Launch each instance in its own terminal:
```bash
NACOS_SERVER=localhost:8848 mvn -pl haifa-openfeign-broadcast-server spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=8081"

# in another terminal
NACOS_SERVER=localhost:8848 mvn -pl haifa-openfeign-broadcast-server spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=8082"
```

## Run the Client
```bash
NACOS_SERVER=localhost:8848 mvn -pl haifa-openfeign-broadcast-client spring-boot:run
```
The client registers as `broadcast-client` and exposes REST endpoints on `http://localhost:8090`.

## Verify Broadcast Behaviour
Trigger a broadcast call and inspect the aggregated result plus per-instance state:
```bash
curl -X POST http://localhost:8090/api/broadcast \
  -H 'Content-Type: application/json' \
  -d '{"message":"hello-openfeign"}'
```
Response shape:
```json
{
  "result": {
    "serviceId": "broadcast-provider",
    "failFast": false,
    "instances": [
      {
        "instanceId": "broadcast-provider:8081",
        "status": 200,
        "success": true,
        "body": "{\"instanceId\":\"broadcast-provider:8081\",\"totalMessages\":1,\"processedAt\":\"2024-01-01T00:00:00Z\"}"
      }
    ]
  },
  "instanceMessages": {
    "broadcast-provider:8081": [
      {
        "message": "hello-openfeign",
        "origin": "broadcast-client",
        "receivedAt": "2024-01-01T00:00:00Z"
      }
    ]
  }
}
```

Additional helper endpoint:
- `GET http://localhost:8090/api/broadcast/instances` – current provider instances discovered via Nacos.

## Clean Up
```bash
docker compose down
```

## Notes
- The broadcast client aggregates per-instance responses into a JSON payload (`BroadcastResult`), returning HTTP `207` (Multi-Status) when at least one instance fails.
- Providers keep messages in-memory only; restart the service to clear history.
- Adjust the Nacos address via `NACOS_SERVER` environment variable if you run the registry somewhere else.
- Server and client profiles respect the `SERVER_PORT` environment variable, making it easy to run multiple instances from scripts.
- Build only the reusable components with:
  - `mvn -pl haifa-openfeign-broadcast-rpc -am package` for the RPC contract jar.
  - `mvn -pl haifa-openfeign-broadcast-extension -am package` for the broadcast OpenFeign extension jar.
