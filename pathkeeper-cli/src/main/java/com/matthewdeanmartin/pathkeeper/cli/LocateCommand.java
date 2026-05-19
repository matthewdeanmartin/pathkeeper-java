package com.matthewdeanmartin.pathkeeper.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(
    name = "locate",
    description = "Find an executable in PATH and show which entry provides it.",
    mixinStandardHelpOptions = true
)
public class LocateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Executable name to locate")
    String name;

    @Option(names = {"--all"}, description = "Show all matching entries, not just the first")
    boolean all;

    @Override
    public Integer call() {
        System.err.println("[locate] Not yet implemented.");
        return 1;
    }
}
