package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.backup.BackupRecord;
import com.matthewdeanmartin.pathkeeper.backup.BackupStore;
import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;
import com.matthewdeanmartin.pathkeeper.diff.PathDiff;
import com.matthewdeanmartin.pathkeeper.platform.PathReaders;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

@Command(
    name = "diff-current",
    description = "Show the diff between a backup and the current PATH.",
    mixinStandardHelpOptions = true
)
public class DiffCurrentCommand implements Callable<Integer> {

    @ParentCommand PathkeeperCommand parent;

    @Parameters(index = "0", description = "Backup identifier (default: latest)", arity = "0..1", defaultValue = "")
    String identifier;

    @Override
    public Integer call() throws Exception {
        AppDirs.ensureAppState();
        String osName = Diagnostics.normalizedOsName();
        var backupDir = AppDirs.backupsHome();

        BackupRecord record = BackupStore.resolve(identifier, backupDir);
        Snapshot current   = PathReaders.create().readSnapshotVar(parent.varName);

        var backupEntries  = Diagnostics.entriesForScope(Scope.ALL, record.systemPath, record.userPath);
        var currentEntries = Diagnostics.entriesForScope(Scope.ALL, current.systemPath(), current.userPath());

        PathDiff diff = PathDiff.compute(backupEntries, currentEntries, osName);
        System.out.println(diff.render());
        return 0;
    }
}
