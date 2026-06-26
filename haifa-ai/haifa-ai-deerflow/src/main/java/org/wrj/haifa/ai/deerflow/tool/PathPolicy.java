package org.wrj.haifa.ai.deerflow.tool;

import java.io.IOException;
import java.nio.file.Path;

public final class PathPolicy {

    private PathPolicy() {
    }

    public static Path resolveInsideWorkspace(Path workspaceRoot, String requestedPath) {
        Path root = workspaceRoot.toAbsolutePath().normalize();
        Path resolved = root.resolve(requestedPath == null ? "" : requestedPath).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Path escapes workspace: " + requestedPath);
        }
        return resolved;
    }

    public static Path resolveExistingInsideWorkspace(Path workspaceRoot, String requestedPath) throws IOException {
        Path root = workspaceRoot.toRealPath();
        Path resolved = root.resolve(requestedPath == null ? "" : requestedPath).normalize();
        Path realResolved = resolved.toRealPath();
        if (!realResolved.startsWith(root)) {
            throw new IllegalArgumentException("Path escapes workspace: " + requestedPath);
        }
        return realResolved;
    }
}
