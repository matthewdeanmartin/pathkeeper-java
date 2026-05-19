package com.matthewdeanmartin.pathkeeper.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(
    name = "diff-current",
    description = "Show the diff between a backup and the current PATH.",
    mixinStandardHelpOptions = true
)
public class DiffCurrentCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Backup identifier (default: latest)", arity = "0..1", defaultValue = "")
    String identifier;

    @Override
    public Integer call() {
        System.err.println("[diff-current] Not yet implemented.");
        return 1;
    }
}
