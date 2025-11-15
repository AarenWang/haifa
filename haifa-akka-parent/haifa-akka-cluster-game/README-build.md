# haifa-akka-cluster-game: build helper

This module contains three submodules: `haifa-akka-cluster-game-common`, `haifa-akka-cluster-game-node`, and `haifa-akka-cluster-game-gateway`.

If you see errors like "The POM for me.wrj:haifa-akka-cluster-game-common:jar:1.0-SNAPSHOT is missing", run the following from the `haifa-akka-cluster-game` directory to build required modules first:

```bash
# build all submodules and install to local repo
mvn -T1C clean install -DskipTests

# or build only gateway and its dependencies (and install them for later runs)
mvn -pl haifa-akka-cluster-game-gateway -am clean install -DskipTests
```

The parent hierarchy expects the top-level `haifa-parent` to be accessible via relative path during nested builds; if you build from a nested folder, Maven should still locate the parent POM thanks to `relativePath` entries.
