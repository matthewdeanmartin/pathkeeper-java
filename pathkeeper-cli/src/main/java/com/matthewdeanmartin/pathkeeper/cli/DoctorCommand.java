package com.matthewdeanmartin.pathkeeper.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
    name = "doctor",
    description = "Run health checks on the current PATH.",
    mixinStandardHelpOptions = true
)
public class DoctorCommand implements Callable<Integer> {

    @Option(names = {"--explain"}, description = "Include remediation advice for each issue")
    boolean explain;

    @Option(names = {"--scope"}, description = "system, user, or all (default: all)", defaultValue = "all")
    String scope;

    @Option(names = {"--json"}, description = "Output as JSON")
    boolean json;

    @Override
    public Integer call() {
        System.err.println("[doctor] Not yet implemented.");
        return 1;
    }
}
