package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.backup.BackupStore;
import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;
import com.matthewdeanmartin.pathkeeper.platform.PathReaders;
import com.matthewdeanmartin.pathkeeper.splitlong.SplitLong;
import com.matthewdeanmartin.pathkeeper.splitlong.SplitLongPlan;
import com.matthewdeanmartin.pathkeeper.writer.PathWriters;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

@Command(
    name = "split-long",
    description = "Split a long PATH into helper variables (Windows).",
    mixinStandardHelpOptions = true
)
public class SplitLongCommand implements Callable<Integer> {

    @ParentCommand PathkeeperCommand parent;

    @Option(names = {"--dry-run"}, description = "Show the chunking plan without writing")
    boolean dryRun;

    @Option(names = {"--force"}, description = "Apply even if PATH is within limits")
    boolean force;

    @Option(names = {"--scope"}, description = "system or user (default: user)", defaultValue = "user")
    String scope;

    @Option(names = {"--max-length"}, description = "PATH length limit in characters (default: 2047)", defaultValue = "2047")
    int maxLength;

    @Option(names = {"--chunk-length"}, description = "Max characters per helper variable (default: 1800)", defaultValue = "1800")
    int chunkLength;

    @Override
    public Integer call() throws Exception {
        AppDirs.ensureAppState();
        String osName = Diagnostics.normalizedOsName();
        Scope s = Scope.parse(scope);
        Snapshot current = PathReaders.create().readSnapshotVar(parent.varName);

        SplitLongPlan plan;
        try {
            plan = SplitLong.build(current, s, osName, maxLength, chunkLength, null);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 1;
        }

        System.out.print(SplitLong.renderPlan(plan));

        if (!plan.changed() && !force) return 0;

        if (!dryRun) {
            Snapshot updated = SplitLong.applyToSnapshot(current, plan);
            BackupStore.create(current, AppDirs.backupsHome(), "auto", "pre-split-long", false);
            PathWriters.writeChanged(PathWriters.create(parent.varName), current, updated, s);
            System.out.println("PATH split applied.");
        } else {
            System.out.println("[dry-run] No changes written.");
        }
        return 0;
    }
}
