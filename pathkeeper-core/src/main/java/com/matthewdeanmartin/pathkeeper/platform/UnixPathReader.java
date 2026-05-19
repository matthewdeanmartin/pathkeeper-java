package com.matthewdeanmartin.pathkeeper.platform;

import com.matthewdeanmartin.pathkeeper.backup.Snapshot;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UnixPathReader implements PathReader {

    @Override
    public Snapshot readSnapshot() {
        return readSnapshotVar("PATH");
    }

    @Override
    public Snapshot readSnapshotVar(String varName) {
        if (varName == null || varName.isBlank()) varName = "PATH";
        String raw = System.getenv(varName);
        if (raw == null) raw = "";
        return new Snapshot(
            Collections.emptyList(),
            splitPath(raw),
            "",
            raw,
            Map.of(),
            Map.of()
        );
    }

    private static List<String> splitPath(String raw) {
        if (raw.isBlank()) return Collections.emptyList();
        return Arrays.asList(raw.split(":", -1));
    }
}
