package com.matthewdeanmartin.pathkeeper.writer;

import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;

import java.io.IOException;
import java.util.List;

public final class PathWriters {

    private PathWriters() {}

    public static PathWriter create(String varName) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return new WindowsPathWriter(varName);
        return new UnixPathWriter(varName);
    }

    /** Write only the scopes that actually changed. */
    public static void writeChanged(PathWriter writer, Snapshot current, Snapshot updated, Scope scope)
        throws IOException {
        if (scope == Scope.SYSTEM || scope == Scope.ALL) {
            if (!current.systemPath().equals(updated.systemPath())) {
                writer.writeSystemPath(updated.systemPath());
            }
        }
        if (scope == Scope.USER || scope == Scope.ALL) {
            if (!current.userPath().equals(updated.userPath())) {
                writer.writeUserPath(updated.userPath());
            }
        }
    }

    public static String joinPath(List<String> entries, String osName) {
        return String.join("windows".equals(osName) ? ";" : ":", entries);
    }
}
