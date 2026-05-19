package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import com.matthewdeanmartin.pathkeeper.diagnostics.DiagnosticEntry;
import com.matthewdeanmartin.pathkeeper.diagnostics.DiagnosticReport;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;
import com.matthewdeanmartin.pathkeeper.platform.PathReaders;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "inspect",
    description = "Show all PATH entries with validity and duplicate information.",
    mixinStandardHelpOptions = true
)
public class InspectCommand implements Callable<Integer> {

    @ParentCommand PathkeeperCommand parent;

    @Option(names = {"--only-invalid"}, description = "Show only invalid entries")
    boolean onlyInvalid;

    @Option(names = {"--only-dupes"}, description = "Show only duplicate entries")
    boolean onlyDupes;

    @Option(names = {"--scope"}, description = "system, user, or all (default: all)", defaultValue = "all")
    String scope;

    @Option(names = {"--json"}, description = "Output as JSON")
    boolean json;

    @Override
    public Integer call() throws Exception {
        AppDirs.ensureAppState();
        String osName = Diagnostics.normalizedOsName();
        Scope s = Scope.parse(scope);
        Snapshot snapshot = PathReaders.create().readSnapshotVar(parent.varName);
        String rawValue = Diagnostics.rawForScope(s, snapshot.systemPathRaw(), snapshot.userPathRaw(), osName);
        DiagnosticReport report = Diagnostics.analyzeSnapshot(
            snapshot.systemPath(), snapshot.userPath(), osName, s, rawValue);

        List<DiagnosticEntry> entries = report.entries().stream()
            .filter(e -> !onlyInvalid || (!e.exists() || !e.isDir()) && !e.isEmpty())
            .filter(e -> !onlyDupes  || e.isDuplicate())
            .toList();

        System.out.printf("%-5s %-8s %-6s %-5s %-5s  %s%n",
            "#", "scope", "exists", "dir", "dupe", "path");
        System.out.println("-".repeat(80));

        for (DiagnosticEntry e : entries) {
            System.out.printf("%-5d %-8s %-6s %-5s %-5s %s%n",
                e.index(),
                e.scope().name().toLowerCase(),
                e.exists() ? "yes" : "no",
                e.isDir()  ? "yes" : (e.isEmpty() ? "-" : "no"),
                e.isDuplicate() ? "dup" : "",
                e.value().isEmpty() ? "<empty>" : e.value()
            );
        }

        System.out.println();
        var sum = report.summary();
        System.out.printf("Total: %d  Valid: %d  Invalid: %d  Duplicates: %d  Empty: %d%n",
            sum.total(), sum.valid(), sum.invalid(), sum.duplicates(), sum.empty());
        for (String w : sum.warnings()) {
            System.out.println("WARNING: " + w);
        }
        return 0;
    }
}
