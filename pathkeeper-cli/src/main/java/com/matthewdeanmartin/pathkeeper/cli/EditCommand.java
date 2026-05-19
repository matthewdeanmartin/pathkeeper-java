package com.matthewdeanmartin.pathkeeper.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "edit",
    description = "Non-interactively edit PATH entries (add, remove, move, replace).",
    mixinStandardHelpOptions = true
)
public class EditCommand implements Callable<Integer> {

    @Option(names = {"--add"}, description = "Directory to add")
    List<String> add;

    @Option(names = {"--remove"}, description = "Directory to remove")
    List<String> remove;

    @Option(names = {"--move-up"}, description = "Move entry up one position")
    String moveUp;

    @Option(names = {"--move-down"}, description = "Move entry down one position")
    String moveDown;

    @Option(names = {"--replace"}, description = "Replace an entry: OLD=NEW", arity = "1")
    String replace;

    @Option(names = {"--scope"}, description = "system, user, or all (default: user)", defaultValue = "user")
    String scope;

    @Option(names = {"--dry-run"}, description = "Show what would change without writing")
    boolean dryRun;

    @Override
    public Integer call() {
        System.err.println("[edit] Not yet implemented.");
        return 1;
    }
}
