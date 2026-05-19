package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;
import com.matthewdeanmartin.pathkeeper.platform.PathReaders;
import com.matthewdeanmartin.pathkeeper.shadow.ShadowGroup;
import com.matthewdeanmartin.pathkeeper.shadow.Shadows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "shadow",
    description = "Find executables shadowed by earlier PATH entries.",
    mixinStandardHelpOptions = true
)
public class ShadowCommand implements Callable<Integer> {

    @ParentCommand PathkeeperCommand parent;

    @Parameters(index = "0", description = "Executable name to check (optional)", arity = "0..1")
    String name;

    @Option(names = {"--json"}, description = "Output as JSON")
    boolean json;

    @Override
    public Integer call() throws Exception {
        AppDirs.ensureAppState();
        String osName = Diagnostics.normalizedOsName();
        Snapshot snapshot = PathReaders.create().readSnapshotVar(parent.varName);
        List<ShadowGroup> groups = Shadows.findShadows(
            snapshot.systemPath(), snapshot.userPath(), osName, Scope.ALL);

        if (name != null && !name.isBlank()) {
            String key = ("windows".equals(osName) || "darwin".equals(osName)) ? name.toLowerCase() : name;
            groups = groups.stream().filter(g -> g.name().equals(key)).toList();
        }

        if (groups.isEmpty()) {
            System.out.println("No shadowed executables found.");
            return 0;
        }

        for (ShadowGroup g : groups) {
            System.out.println(g.name());
            System.out.printf("  winner:  [%d] %s (%s)%n",
                g.winner().index(), g.winner().directory(), g.winner().scope().name().toLowerCase());
            for (var s : g.shadowed()) {
                System.out.printf("  shadowed:[%d] %s (%s)%n",
                    s.index(), s.directory(), s.scope().name().toLowerCase());
            }
        }
        return 0;
    }
}
