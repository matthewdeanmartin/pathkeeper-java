package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import com.matthewdeanmartin.pathkeeper.platform.PathReaders;
import com.matthewdeanmartin.pathkeeper.selfcheck.Selfcheck;
import com.matthewdeanmartin.pathkeeper.selfcheck.SelfcheckResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "selfcheck",
    description = "Verify pathkeeper installation health.",
    mixinStandardHelpOptions = true
)
public class SelfcheckCommand implements Callable<Integer> {

    @Option(names = {"--json"}, description = "Output as JSON")
    boolean json;

    @Override
    public Integer call() throws Exception {
        AppDirs.ensureAppState();
        List<SelfcheckResult> results = Selfcheck.run(PathReaders.create());

        for (SelfcheckResult r : results) {
            String icon = switch (r.status()) {
                case SelfcheckResult.PASS -> "[PASS]";
                case SelfcheckResult.FAIL -> "[FAIL]";
                default                   -> "[WARN]";
            };
            System.out.printf("%s  %-30s %s%n", icon, r.name(), r.detail());
            if (!r.remediation().isEmpty() && !SelfcheckResult.PASS.equals(r.status())) {
                System.out.println("       -> " + r.remediation());
            }
        }
        return Selfcheck.allPassed(results) ? 0 : 1;
    }
}
