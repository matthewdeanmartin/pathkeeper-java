package com.matthewdeanmartin.pathkeeper.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(
    name = "restore",
    description = "Restore PATH from a backup.",
    mixinStandardHelpOptions = true
)
public class RestoreCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Backup identifier (index, filename prefix, or path)", arity = "0..1", defaultValue = "")
    String identifier;

    @Option(names = {"--dry-run"}, description = "Show what would change without writing")
    boolean dryRun;

    @Option(names = {"--scope"}, description = "system, user, or all (default: all)", defaultValue = "all")
    String scope;

    @Override
    public Integer call() {
        System.err.println("[restore] Not yet implemented.");
        return 1;
    }
}
