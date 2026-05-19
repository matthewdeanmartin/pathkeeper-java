package com.matthewdeanmartin.pathkeeper.backup;

import java.util.List;
import java.util.Map;

public record Snapshot(
    List<String> systemPath,
    List<String> userPath,
    String systemPathRaw,
    String userPathRaw,
    Map<String, String> systemEnvVars,
    Map<String, String> userEnvVars
) {
    public static Snapshot empty() {
        return new Snapshot(List.of(), List.of(), "", "", Map.of(), Map.of());
    }
}
