package com.matthewdeanmartin.pathkeeper.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
    name = "dedupe",
    description = "Remove duplicate PATH entries.",
    mixinStandardHelpOptions = true
)
public class DedupeCommand implements Callable<Integer> {

    @Option(names = {"--keep"}, description = "Which duplicate to keep: first or last (default: first)", defaultValue = "first")
    String keep;

    @Option(names = {"--remove-invalid"}, description = "Also remove invalid (non-existent) entries")
    boolean removeInvalid;

    @Option(names = {"--scope"}, description = "system, user, or all (default: all)", defaultValue = "all")
    String scope;

    @Option(names = {"--dry-run"}, description = "Show what would change without writing")
    boolean dryRun;

    @Override
    public Integer call() {
        System.err.println("[dedupe] Not yet implemented.");
        return 1;
    }
}
