package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.shellstartup.ShellStartup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
    name = "shell-startup",
    description = "Inject a pathkeeper backup call into your shell startup file.",
    mixinStandardHelpOptions = true
)
public class ShellStartupCommand implements Callable<Integer> {

    @Option(names = {"--dry-run"}, description = "Show what would be added without writing")
    boolean dryRun;

    @Option(names = {"--remove"}, description = "Remove the hook instead of adding it")
    boolean remove;

    @Override
    public Integer call() throws Exception {
        if (remove) {
            Optional<Path> removed = ShellStartup.uninstall(dryRun);
            if (removed.isPresent()) {
                System.out.println("Hook removed from " + removed.get());
            } else {
                System.out.println("No hook found to remove.");
            }
            return 0;
        }

        // Check current status first
        Optional<Path> existing = ShellStartup.detect();
        if (existing.isPresent()) {
            System.out.println("Hook already installed in " + existing.get());
            return 0;
        }

        Path target = ShellStartup.install(dryRun);
        if (!dryRun) {
            System.out.println("Hook installed in " + target);
        }
        return 0;
    }
}
