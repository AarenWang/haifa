package org.wrj.haifa.ai.deerflow.sandbox;

import java.nio.file.Path;
import java.util.List;

public record HostShell(Path executable, Kind kind) {

    public enum Kind {
        BASH,
        GIT_BASH
    }

    public List<String> command(String script, boolean login) {
        return List.of(executable.toString(), login ? "-lc" : "-c", script);
    }
}
