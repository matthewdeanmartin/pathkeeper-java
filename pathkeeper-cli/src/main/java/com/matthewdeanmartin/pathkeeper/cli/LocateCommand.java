package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.locate.Locator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
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
    public Integer call() throws Exception {
        AppDirs.ensureAppState();
        String osName = Diagnostics.normalizedOsName();
        List<String> found = Locator.locate(name, all, osName);

        if (found.isEmpty()) {
            System.out.println(name + ": not found");
            return 1;
        }
        found.forEach(System.out::println);
        return 0;
    }
}
