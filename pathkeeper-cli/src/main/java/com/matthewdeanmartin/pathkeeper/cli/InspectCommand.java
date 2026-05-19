package com.matthewdeanmartin.pathkeeper.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
    name = "inspect",
    description = "Show all PATH entries with validity and duplicate information.",
    mixinStandardHelpOptions = true
)
public class InspectCommand implements Callable<Integer> {

    @Option(names = {"--only-invalid"}, description = "Show only invalid entries")
    boolean onlyInvalid;

    @Option(names = {"--only-dupes"}, description = "Show only duplicate entries")
    boolean onlyDupes;

    @Option(names = {"--scope"}, description = "system, user, or all (default: all)", defaultValue = "all")
    String scope;

    @Option(names = {"--json"}, description = "Output as JSON")
    boolean json;

    @Override
    public Integer call() {
        System.err.println("[inspect] Not yet implemented.");
        return 1;
    }
}
