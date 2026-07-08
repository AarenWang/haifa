package org.wrj.haifa.ai.deerflow.config;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserDataDirectoryInitializer {

    private static final Logger log = LoggerFactory.getLogger(UserDataDirectoryInitializer.class);

    private final DeerFlowProperties properties;

    public UserDataDirectoryInitializer(DeerFlowProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        create(properties.getUserDataRoot(), "user data root");
        create(properties.getUploadsRoot(), "uploads root");
        create(properties.getWorkspaceRoot(), "workspace root");
        create(properties.getOutputsRoot(), "outputs root");
    }

    private void create(String path, String label) {
        try {
            Files.createDirectories(Path.of(path).toAbsolutePath().normalize());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create " + label + ": " + path, ex);
        }
        log.debug("Ensured DeerFlow {} exists at {}", label, path);
    }
}