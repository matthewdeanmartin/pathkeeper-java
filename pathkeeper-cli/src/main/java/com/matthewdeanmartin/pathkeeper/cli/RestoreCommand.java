package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.backup.BackupRecord;
import com.matthewdeanmartin.pathkeeper.backup.BackupStore;
import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;
import com.matthewdeanmartin.pathkeeper.diff.PathDiff;
import com.matthewdeanmartin.pathkeeper.platform.PathReaders;
import com.matthewdeanmartin.pathkeeper.writer.PathWriters;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
    name = "restore",
    description = "Restore PATH from a backup.",
    mixinStandardHelpOptions = true
)
public class RestoreCommand implements Callable<Integer> {

    @ParentCommand PathkeeperCommand parent;

    @Parameters(index = "0", description = "Backup identifier (index, filename prefix, or path)", arity = "0..1", defaultValue = "")
    String identifier;

    @Option(names = {"--dry-run"}, description = "Show what would change without writing")
    boolean dryRun;

    @Option(names = {"--scope"}, description = "system, user, or all (default: all)", defaultValue = "all")
    String scope;

    @Override
    public Integer call() throws Exception {
        AppDirs.ensureAppState();
        String osName = Diagnostics.normalizedOsName();
        Scope s = Scope.parse(scope);
        Path backupDir = AppDirs.backupsHome();

        BackupRecord record  = BackupStore.resolve(identifier, backupDir);
        Snapshot current     = PathReaders.create().readSnapshotVar(parent.varName);

        // Pre-restore backup
        Optional<Path> preBackup = BackupStore.create(current, backupDir, "auto", "pre-restore", false);
        preBackup.ifPresent(p -> System.out.println("Pre-restore backup: " + p));

        List<String> restoredSystem = (s == Scope.USER) ? current.systemPath() : record.systemPath;
        List<String> restoredUser   = (s == Scope.SYSTEM) ? current.userPath() : record.userPath;
        Snapshot updated = new Snapshot(restoredSystem, restoredUser,
            record.systemPathRaw, record.userPathRaw,
            current.systemEnvVars(), current.userEnvVars());

        PathDiff diff = PathDiff.compute(
            Diagnostics.entriesForScope(s, current.systemPath(), current.userPath()),
            Diagnostics.entriesForScope(s, restoredSystem, restoredUser),
            osName);

        System.out.println(diff.isEmpty() ? "No changes." : diff.render());

        if (!dryRun && !diff.isEmpty()) {
            PathWriters.writeChanged(PathWriters.create(parent.varName), current, updated, s);
            System.out.println("PATH restored.");
        } else if (dryRun) {
            System.out.println("[dry-run] No changes written.");
        }
        return 0;
    }
}
