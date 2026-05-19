package com.matthewdeanmartin.pathkeeper.gui;

import com.matthewdeanmartin.pathkeeper.backup.BackupRecord;
import com.matthewdeanmartin.pathkeeper.backup.BackupStore;
import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.platform.PathReaders;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

class AppState {

    final String osName = Diagnostics.normalizedOsName();
    final String varName = "PATH";

    Snapshot snapshot() throws IOException {
        return PathReaders.create().readSnapshotVar(varName);
    }

    List<BackupRecord> backups() throws IOException {
        return BackupStore.list(backupsDir());
    }

    Path backupsDir() {
        return AppDirs.backupsHome();
    }
}
