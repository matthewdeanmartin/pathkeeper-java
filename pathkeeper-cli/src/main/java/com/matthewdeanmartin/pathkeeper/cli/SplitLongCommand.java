package com.matthewdeanmartin.pathkeeper.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
    name = "split-long",
    description = "Split a long PATH into helper variables (Windows).",
    mixinStandardHelpOptions = true
)
public class SplitLongCommand implements Callable<Integer> {

    @Option(names = {"--dry-run"}, description = "Show the chunking plan without writing")
    boolean dryRun;

    @Option(names = {"--force"}, description = "Apply even if PATH is within limits")
    boolean force;

    @Override
    public Integer call() {
        System.err.println("[split-long] Not yet implemented.");
        return 1;
    }
}
