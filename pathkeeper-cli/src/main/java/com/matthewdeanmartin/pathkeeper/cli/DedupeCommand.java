package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.backup.BackupStore;
import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import com.matthewdeanmartin.pathkeeper.dedupe.Deduper;
import com.matthewdeanmartin.pathkeeper.dedupe.DedupeResult;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;
import com.matthewdeanmartin.pathkeeper.platform.PathReaders;
import com.matthewdeanmartin.pathkeeper.writer.PathWriters;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;

@Command(
    name = "dedupe",
    description = "Remove duplicate PATH entries.",
    mixinStandardHelpOptions = true
)
public class DedupeCommand implements Callable<Integer> {

    @ParentCommand PathkeeperCommand parent;

    @Option(names = {"--keep"}, description = "Which duplicate to keep: first or last (default: first)", defaultValue = "first")
    String keep;

    @Option(names = {"--remove-invalid"}, description = "Also remove invalid (non-existent) entries")
    boolean removeInvalid;

    @Option(names = {"--scope"}, description = "system, user, or all (default: all)", defaultValue = "all")
    String scope;

    @Option(names = {"--dry-run"}, description = "Show what would change without writing")
    boolean dryRun;

    @Override
    public Integer call() throws Exception {
        AppDirs.ensureAppState();
        String osName = Diagnostics.normalizedOsName();
        Scope s = Scope.parse(scope);
        Snapshot current = PathReaders.create().readSnapshotVar(parent.varName);
        Path backupDir = AppDirs.backupsHome();

        Set<String> preSeen = new HashSet<>();
        List<String> newSystem = current.systemPath();
        List<String> newUser   = current.userPath();

        if (s == Scope.SYSTEM || s == Scope.ALL) {
            DedupeResult r = Deduper.dedupe(current.systemPath(), osName, keep, removeInvalid, preSeen);
            newSystem = r.cleaned();
            r.cleaned().forEach(e -> preSeen.add(Diagnostics.canonicalizeEntry(e, osName)));
            printResult("system", r);
        }
        if (s == Scope.USER || s == Scope.ALL) {
            DedupeResult r = Deduper.dedupe(current.userPath(), osName, keep, removeInvalid, preSeen);
            newUser = r.cleaned();
            printResult("user", r);
        }

        Snapshot updated = new Snapshot(newSystem, newUser,
            PathWriters.joinPath(newSystem, osName), PathWriters.joinPath(newUser, osName),
            current.systemEnvVars(), current.userEnvVars());

        if (!dryRun) {
            BackupStore.create(current, backupDir, "auto", "pre-dedupe", false);
            PathWriters.writeChanged(PathWriters.create(parent.varName), current, updated, s);
            System.out.println("PATH updated.");
        } else {
            System.out.println("[dry-run] No changes written.");
        }
        return 0;
    }

    private void printResult(String scopeName, DedupeResult r) {
        if (!r.removedDuplicates().isEmpty()) {
            System.out.println("Duplicates removed from " + scopeName + ":");
            r.removedDuplicates().forEach(e -> System.out.println("  - " + e));
        }
        if (!r.removedInvalid().isEmpty()) {
            System.out.println("Invalid entries removed from " + scopeName + ":");
            r.removedInvalid().forEach(e -> System.out.println("  - " + e));
        }
        if (!r.removedEmpty().isEmpty()) {
            System.out.println("Empty entries removed from " + scopeName + ": " + r.removedEmpty().size());
        }
    }
}
