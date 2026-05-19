package com.matthewdeanmartin.pathkeeper.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
    name = "runtime-entries",
    description = "Detect PATH entries injected only at runtime (not in registry/rc files).",
    mixinStandardHelpOptions = true
)
public class RuntimeEntriesCommand implements Callable<Integer> {

    @Option(names = {"--json"}, description = "Output as JSON")
    boolean json;

    @Override
    public Integer call() {
        System.err.println("[runtime-entries] Not yet implemented.");
        return 1;
    }
}
