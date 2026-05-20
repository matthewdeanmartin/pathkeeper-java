package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.backup.BackupStore;
import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import com.matthewdeanmartin.pathkeeper.platform.PathReaders;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
    name = "backup",
    description = "Create a backup of the current PATH.",
    mixinStandardHelpOptions = true
)
public class BackupCommand implements Callable<Integer> {

    @ParentCommand PathkeeperCommand parent;

    @Option(names = {"--note", "-n"}, description = "Human-readable note for this backup")
    String note;

    @Option(names = {"--tag", "-t"}, description = "Tag (default: manual)", defaultValue = "manual")
    String tag;

    @Option(names = {"--force", "-f"}, description = "Create even if PATH is unchanged")
    boolean force;

    @Option(names = {"--dry-run"}, description = "Show what would be backed up without writing")
    boolean dryRun;

    @Override
    public Integer call() throws Exception {
        AppDirs.ensureAppState();
        Snapshot snapshot = PathReaders.create().readSnapshotVar(parent.varName);
        Path backupDir = AppDirs.backupsHome();

        if (dryRun) {
            System.out.println("Would create a backup for " + parent.varName + " in " + backupDir);
            System.out.println("System entries: " + snapshot.systemPath().size());
            System.out.println("User entries: " + snapshot.userPath().size());
            return 0;
        }

        Optional<Path> result = BackupStore.create(snapshot, backupDir, tag, note, force);
        if (result.isPresent()) {
            System.out.println("Backup created: " + result.get());
        } else {
            System.out.println("No change detected; backup skipped (use --force to override).");
        }
        return 0;
    }
}
