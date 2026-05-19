package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.platform.PathReaders;
import com.matthewdeanmartin.pathkeeper.runtime.RuntimeEntries;
import com.matthewdeanmartin.pathkeeper.runtime.RuntimeEntry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "runtime-entries",
    description = "Detect PATH entries injected only at runtime (not in registry/rc files).",
    mixinStandardHelpOptions = true
)
public class RuntimeEntriesCommand implements Callable<Integer> {

    @ParentCommand PathkeeperCommand parent;

    @Option(names = {"--json"}, description = "Output as JSON")
    boolean json;

    @Override
    public Integer call() throws Exception {
        AppDirs.ensureAppState();
        String osName = Diagnostics.normalizedOsName();
        Snapshot snapshot = PathReaders.create().readSnapshotVar(parent.varName);
        List<RuntimeEntry> entries = RuntimeEntries.detect(
            snapshot.systemPath(), snapshot.userPath(), osName);

        System.out.printf("%-8s %-10s %s%n", "persisted", "scope", "path");
        System.out.println("-".repeat(70));
        for (RuntimeEntry e : entries) {
            String scope = e.scope() != null ? e.scope().name().toLowerCase() : "runtime";
            System.out.printf("%-8s %-10s %s%n", e.persisted() ? "yes" : "no", scope, e.value());
        }

        long runtimeOnly = entries.stream().filter(RuntimeEntry::isRuntimeOnly).count();
        System.out.println();
        System.out.println("Runtime-only entries: " + runtimeOnly);
        return 0;
    }
}
