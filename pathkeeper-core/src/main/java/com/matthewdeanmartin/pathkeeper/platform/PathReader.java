package com.matthewdeanmartin.pathkeeper.platform;

import com.matthewdeanmartin.pathkeeper.backup.Snapshot;

import java.io.IOException;

public interface PathReader {
    Snapshot readSnapshot() throws IOException;
    Snapshot readSnapshotVar(String varName) throws IOException;
}
