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

    @Override
    public Integer call() throws Exception {
        AppDirs.ensureAppState();
        Snapshot snapshot = PathReaders.create().readSnapshotVar(parent.varName);
        Path backupDir = AppDirs.backupsHome();

        Optional<Path> result = BackupStore.create(snapshot, backupDir, tag, note, force);
        if (result.isPresent()) {
            System.out.println("Backup created: " + result.get());
        } else {
            System.out.println("No change detected; backup skipped (use --force to override).");
        }
        return 0;
    }
}
