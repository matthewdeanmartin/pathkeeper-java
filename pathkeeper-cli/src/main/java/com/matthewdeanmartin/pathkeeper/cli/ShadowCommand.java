package com.matthewdeanmartin.pathkeeper.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(
    name = "shadow",
    description = "Find executables shadowed by earlier PATH entries.",
    mixinStandardHelpOptions = true
)
public class ShadowCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Executable name to check (optional)", arity = "0..1")
    String name;

    @Option(names = {"--json"}, description = "Output as JSON")
    boolean json;

    @Override
    public Integer call() {
        System.err.println("[shadow] Not yet implemented.");
        return 1;
    }
}
