package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.backup.BackupRecord;
import com.matthewdeanmartin.pathkeeper.backup.BackupStore;
import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;
import com.matthewdeanmartin.pathkeeper.diff.PathDiff;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

@Command(
    name = "diff",
    description = "Show the diff between two backups.",
    mixinStandardHelpOptions = true
)
public class DiffCommand implements Callable<Integer> {

    @ParentCommand PathkeeperCommand parent;

    @Parameters(index = "0", description = "First backup identifier")
    String left;

    @Parameters(index = "1", description = "Second backup identifier")
    String right;

    @Override
    public Integer call() throws Exception {
        AppDirs.ensureAppState();
        String osName = Diagnostics.normalizedOsName();
        var backupDir = AppDirs.backupsHome();

        BackupRecord leftRecord  = BackupStore.resolve(left,  backupDir);
        BackupRecord rightRecord = BackupStore.resolve(right, backupDir);

        var leftEntries  = Diagnostics.entriesForScope(Scope.ALL, leftRecord.systemPath,  leftRecord.userPath);
        var rightEntries = Diagnostics.entriesForScope(Scope.ALL, rightRecord.systemPath, rightRecord.userPath);

        PathDiff diff = PathDiff.compute(leftEntries, rightEntries, osName);
        System.out.println(diff.render());
        return 0;
    }
}
