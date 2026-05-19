package com.matthewdeanmartin.pathkeeper.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
    name = "populate",
    description = "Discover and add known tool directories to PATH.",
    mixinStandardHelpOptions = true
)
public class PopulateCommand implements Callable<Integer> {

    @Option(names = {"--dry-run"}, description = "Show what would be added without writing")
    boolean dryRun;

    @Option(names = {"--all"}, description = "Include all tool groups, not just common ones")
    boolean all;

    @Option(names = {"--force"}, description = "Add even if directory is already present")
    boolean force;

    @Override
    public Integer call() {
        System.err.println("[populate] Not yet implemented.");
        return 1;
    }
}
