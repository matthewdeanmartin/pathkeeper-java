package com.matthewdeanmartin.pathkeeper.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(
    name = "diff",
    description = "Show the diff between two backups.",
    mixinStandardHelpOptions = true
)
public class DiffCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "First backup identifier")
    String left;

    @Parameters(index = "1", description = "Second backup identifier")
    String right;

    @Override
    public Integer call() {
        System.err.println("[diff] Not yet implemented.");
        return 1;
    }
}
