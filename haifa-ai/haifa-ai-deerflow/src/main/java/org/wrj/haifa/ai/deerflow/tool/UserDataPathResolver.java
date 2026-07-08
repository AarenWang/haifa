package org.wrj.haifa.ai.deerflow.tool;

import java.nio.file.Path;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class UserDataPathResolver {

    public static final String VIRTUAL_UPLOADS_ROOT = "/mnt/user-data/uploads";
    public static final String VIRTUAL_WORKSPACE_ROOT = "/mnt/user-data/workspace";
    public static final String VIRTUAL_OUTPUTS_ROOT = "/mnt/user-data/outputs";

    private final DeerFlowProperties properties;

    public UserDataPathResolver(DeerFlowProperties properties) {
        this.properties = properties;
    }

    public Path uploadsRoot() {
        return Path.of(properties.getUploadsRoot()).toAbsolutePath().normalize();
    }

    public Path workspaceRoot() {
        return Path.of(properties.getWorkspaceRoot()).toAbsolutePath().normalize();
    }

    public Path outputsRoot() {
        return Path.of(properties.getOutputsRoot()).toAbsolutePath().normalize();
    }

    public Path skillsRoot() {
        return Path.of(properties.getSkillsRoot()).toAbsolutePath().normalize();
    }

    public Path resolveReadable(String requestedPath, Path requestWorkspaceRoot) {
        Path resolved = resolveUserDataPath(requestedPath, requestWorkspaceRoot);
        Path abs = resolved.toAbsolutePath().normalize();
        if (isUnder(abs, workspaceRoot()) || isUnder(abs, uploadsRoot()) || isUnder(abs, outputsRoot()) || isUnder(abs, skillsRoot())) {
            return abs;
        }
        throw new IllegalArgumentException("path is outside allowed directories");
    }

    public Path resolveWritable(String requestedPath, Path requestWorkspaceRoot) {
        Path resolved = resolveUserDataPath(requestedPath, requestWorkspaceRoot);
        Path abs = resolved.toAbsolutePath().normalize();
        if (isUnder(abs, workspaceRoot()) || isUnder(abs, outputsRoot())) {
            return abs;
        }
        if (isUnder(abs, uploadsRoot())) {
            throw new IllegalArgumentException("uploads directory is written only by the frontend upload service");
        }
        throw new IllegalArgumentException("path is outside writable directories");
    }

    public Path resolveWorkspace(String requestedPath, Path requestWorkspaceRoot) {
        Path root = normalizeWorkspaceRoot(requestWorkspaceRoot);
        Path resolved = root.resolve(cleanRelativePath(requestedPath)).toAbsolutePath().normalize();
        if (!isUnder(resolved, root)) {
            throw new IllegalArgumentException("path escapes workspace");
        }
        return resolved;
    }

    public String toVirtualPath(Path physicalPath) {
        Path abs = physicalPath.toAbsolutePath().normalize();
        if (isUnder(abs, uploadsRoot())) {
            return VIRTUAL_UPLOADS_ROOT + "/" + slash(uploadsRoot().relativize(abs));
        }
        if (isUnder(abs, workspaceRoot())) {
            return VIRTUAL_WORKSPACE_ROOT + "/" + slash(workspaceRoot().relativize(abs));
        }
        if (isUnder(abs, outputsRoot())) {
            return VIRTUAL_OUTPUTS_ROOT + "/" + slash(outputsRoot().relativize(abs));
        }
        return abs.toString();
    }

    private Path resolveUserDataPath(String requestedPath, Path requestWorkspaceRoot) {
        if (!StringUtils.hasText(requestedPath)) {
            throw new IllegalArgumentException("path is required");
        }
        String path = requestedPath.trim().replace('\\', '/');
        if (path.equals(VIRTUAL_UPLOADS_ROOT) || path.startsWith(VIRTUAL_UPLOADS_ROOT + "/")) {
            return uploadsRoot().resolve(path.substring(VIRTUAL_UPLOADS_ROOT.length()).replaceFirst("^/", "")).normalize();
        }
        if (path.equals(VIRTUAL_WORKSPACE_ROOT) || path.startsWith(VIRTUAL_WORKSPACE_ROOT + "/")) {
            return workspaceRoot().resolve(path.substring(VIRTUAL_WORKSPACE_ROOT.length()).replaceFirst("^/", "")).normalize();
        }
        if (path.equals(VIRTUAL_OUTPUTS_ROOT) || path.startsWith(VIRTUAL_OUTPUTS_ROOT + "/")) {
            return outputsRoot().resolve(path.substring(VIRTUAL_OUTPUTS_ROOT.length()).replaceFirst("^/", "")).normalize();
        }
        if (Path.of(path).isAbsolute()) {
            return Path.of(path).toAbsolutePath().normalize();
        }
        if (path.equals("uploads") || path.startsWith("uploads/")) {
            return uploadsRoot().resolve(path.substring("uploads".length()).replaceFirst("^/", "")).normalize();
        }
        if (path.equals("workspace") || path.startsWith("workspace/")) {
            return workspaceRoot().resolve(path.substring("workspace".length()).replaceFirst("^/", "")).normalize();
        }
        if (path.equals("outputs") || path.startsWith("outputs/")) {
            return outputsRoot().resolve(path.substring("outputs".length()).replaceFirst("^/", "")).normalize();
        }
        return normalizeWorkspaceRoot(requestWorkspaceRoot).resolve(cleanRelativePath(path)).normalize();
    }

    private Path normalizeWorkspaceRoot(Path requestWorkspaceRoot) {
        Path root = requestWorkspaceRoot == null ? workspaceRoot() : requestWorkspaceRoot.toAbsolutePath().normalize();
        return root.equals(Path.of(".").toAbsolutePath().normalize()) ? workspaceRoot() : root;
    }

    private static String cleanRelativePath(String requestedPath) {
        String path = requestedPath == null ? "" : requestedPath.trim().replace('\\', '/');
        while (path.startsWith("./")) {
            path = path.substring(2);
        }
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("path is required");
        }
        if (Path.of(path).isAbsolute()) {
            throw new IllegalArgumentException("absolute paths must use /mnt/user-data aliases");
        }
        return path;
    }

    private static boolean isUnder(Path child, Path root) {
        return child.toAbsolutePath().normalize().startsWith(root.toAbsolutePath().normalize());
    }

    private static String slash(Path path) {
        return path.toString().replace('\\', '/');
    }
}