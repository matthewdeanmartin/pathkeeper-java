package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.backup.BackupRecord;
import com.matthewdeanmartin.pathkeeper.backup.BackupStore;
import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;
import com.matthewdeanmartin.pathkeeper.platform.PathReaders;
import com.matthewdeanmartin.pathkeeper.repair.Repair;
import com.matthewdeanmartin.pathkeeper.repair.TruncationRepair;
import com.matthewdeanmartin.pathkeeper.writer.PathWriters;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "repair-truncated",
    description = "Detect and restore truncated PATH entries using backup history.",
    mixinStandardHelpOptions = true
)
public class RepairTruncatedCommand implements Callable<Integer> {

    @ParentCommand PathkeeperCommand parent;

    @Option(names = {"--dry-run"}, description = "Show what would be repaired without writing")
    boolean dryRun;

    @Option(names = {"--force"}, description = "Apply repair even if entries appear valid")
    boolean force;

    @Override
    public Integer call() throws Exception {
        AppDirs.ensureAppState();
        String osName = Diagnostics.normalizedOsName();
        Snapshot current = PathReaders.create().readSnapshotVar(parent.varName);
        List<BackupRecord> records = BackupStore.list(AppDirs.backupsHome());

        List<Repair> repairs = TruncationRepair.findTruncated(current, Scope.ALL, osName, records);

        if (repairs.isEmpty()) {
            System.out.println("No truncated entries detected.");
            return 0;
        }

        for (Repair r : repairs) {
            System.out.printf("[%s #%d] %s%n", r.scope().name().toLowerCase(), r.displayIndex(), r.value());
            r.candidates().forEach(c -> System.out.println("  -> " + c.path() + "  (from " + c.source() + ")"));
        }

        if (!dryRun) {
            // Apply first candidate for each repair
            List<String> newSystem = new ArrayList<>(current.systemPath());
            List<String> newUser   = new ArrayList<>(current.userPath());
            for (Repair r : repairs) {
                if (r.candidates().isEmpty()) continue;
                String fix = r.candidates().get(0).path();
                if (r.scope() == Scope.SYSTEM) newSystem.set(r.scopeIndex(), fix);
                else                            newUser.set(r.scopeIndex(), fix);
            }
            Snapshot updated = new Snapshot(newSystem, newUser,
                PathWriters.joinPath(newSystem, osName), PathWriters.joinPath(newUser, osName),
                current.systemEnvVars(), current.userEnvVars());
            BackupStore.create(current, AppDirs.backupsHome(), "auto", "pre-repair", false);
            PathWriters.writeChanged(PathWriters.create(parent.varName), current, updated, Scope.ALL);
            System.out.println("Repaired " + repairs.size() + " entr" + (repairs.size() == 1 ? "y" : "ies") + ".");
        } else {
            System.out.println("[dry-run] No changes written.");
        }
        return 0;
    }
}
