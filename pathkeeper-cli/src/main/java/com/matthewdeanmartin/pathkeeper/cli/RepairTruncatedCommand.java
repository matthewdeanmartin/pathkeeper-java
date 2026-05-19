package com.matthewdeanmartin.pathkeeper.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
    name = "repair-truncated",
    description = "Detect and restore truncated PATH entries using backup history.",
    mixinStandardHelpOptions = true
)
public class RepairTruncatedCommand implements Callable<Integer> {

    @Option(names = {"--dry-run"}, description = "Show what would be repaired without writing")
    boolean dryRun;

    @Option(names = {"--force"}, description = "Apply repair even if entries appear valid")
    boolean force;

    @Override
    public Integer call() {
        System.err.println("[repair-truncated] Not yet implemented.");
        return 1;
    }
}
