package com.matthewdeanmartin.pathkeeper.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
    name = "selfcheck",
    description = "Verify pathkeeper installation health.",
    mixinStandardHelpOptions = true
)
public class SelfcheckCommand implements Callable<Integer> {

    @Option(names = {"--json"}, description = "Output as JSON")
    boolean json;

    @Override
    public Integer call() {
        System.err.println("[selfcheck] Not yet implemented.");
        return 1;
    }
}
