package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import com.matthewdeanmartin.pathkeeper.diagnostics.DiagnosticReport;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.DoctorCheck;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;
import com.matthewdeanmartin.pathkeeper.platform.PathReaders;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "doctor",
    description = "Run health checks on the current PATH.",
    mixinStandardHelpOptions = true
)
public class DoctorCommand implements Callable<Integer> {

    @ParentCommand PathkeeperCommand parent;

    @Option(names = {"--explain"}, description = "Include remediation advice for each issue")
    boolean explain;

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
        List<DoctorCheck> checks = Diagnostics.doctorChecks(report);

        boolean anyFail = false;
        for (DoctorCheck check : checks) {
            String icon = switch (check.status()) {
                case DoctorCheck.PASS -> "[PASS]";
                case DoctorCheck.FAIL -> "[FAIL]";
                default               -> "[WARN]";
            };
            System.out.printf("%s  %-40s %s%n", icon, check.name(), check.detail());
            if ((explain || !DoctorCheck.PASS.equals(check.status())) && !check.remediation().isEmpty()) {
                System.out.println("       -> " + check.remediation());
            }
            if (DoctorCheck.FAIL.equals(check.status())) anyFail = true;
        }
        return anyFail ? 1 : 0;
    }
}
