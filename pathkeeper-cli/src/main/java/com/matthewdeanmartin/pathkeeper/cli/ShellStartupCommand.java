package com.matthewdeanmartin.pathkeeper.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
    name = "shell-startup",
    description = "Inject a pathkeeper backup call into your shell startup file.",
    mixinStandardHelpOptions = true
)
public class ShellStartupCommand implements Callable<Integer> {

    @Option(names = {"--dry-run"}, description = "Show what would be added without writing")
    boolean dryRun;

    @Override
    public Integer call() {
        System.err.println("[shell-startup] Not yet implemented.");
        return 1;
    }
}
