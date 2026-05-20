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
import picocli.CommandLine.Option;
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

    @Option(names = {"--scope"}, description = "system, user, or all (default: all)", defaultValue = "all")
    String scope;

    @Override
    public Integer call() throws Exception {
        AppDirs.ensureAppState();
        String osName = Diagnostics.normalizedOsName();
        var backupDir = AppDirs.backupsHome();
        Scope selectedScope = Scope.parse(scope);

        BackupRecord record = BackupStore.resolve(identifier, backupDir);
        Snapshot current   = PathReaders.create().readSnapshotVar(parent.varName);

        var backupEntries  = Diagnostics.entriesForScope(selectedScope, record.systemPath, record.userPath);
        var currentEntries = Diagnostics.entriesForScope(selectedScope, current.systemPath(), current.userPath());

        PathDiff diff = PathDiff.compute(backupEntries, currentEntries, osName);
        System.out.println(diff.render());
        return 0;
    }
}
