package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.backup.BackupStore;
import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;
import com.matthewdeanmartin.pathkeeper.platform.PathReaders;
import com.matthewdeanmartin.pathkeeper.populate.CatalogTool;
import com.matthewdeanmartin.pathkeeper.populate.Populate;
import com.matthewdeanmartin.pathkeeper.populate.PopulateMatch;
import com.matthewdeanmartin.pathkeeper.writer.PathWriters;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(
    name = "populate",
    description = "Discover and add known tool directories to PATH.",
    mixinStandardHelpOptions = true
)
public class PopulateCommand implements Callable<Integer> {

    @ParentCommand PathkeeperCommand parent;

    @Option(names = {"--dry-run"}, description = "Show what would be added without writing")
    boolean dryRun;

    @Option(names = {"--all"}, description = "Include all tool groups, not just common ones")
    boolean all;

    @Option(names = {"--force"}, description = "Add even if directory is already present")
    boolean force;

    @Option(names = {"--category"}, description = "Filter by tool category")
    String category;

    @Option(names = {"--list-catalog"}, description = "List known catalog entries without discovering anything")
    boolean listCatalog;

    @Override
    public Integer call() throws Exception {
        AppDirs.ensureAppState();
        String osName = Diagnostics.normalizedOsName();
        Snapshot current = PathReaders.create().readSnapshotVar(parent.varName);

        List<CatalogTool> tools = Populate.loadCatalog();
        if (listCatalog) {
            tools.stream()
                .filter(tool -> tool.os == null || "all".equals(tool.os) || osName.equals(tool.os))
                .filter(tool -> category == null || category.isBlank() || category.equalsIgnoreCase(tool.category))
                .forEach(tool -> System.out.println(tool.category + ": " + tool.name));
            return 0;
        }
        List<String> existing = force ? List.of()
            : Diagnostics.entriesForScope(Scope.ALL, current.systemPath(), current.userPath());
        List<PopulateMatch> matches = Populate.discover(tools, existing, osName, category);

        if (matches.isEmpty()) {
            System.out.println("No new tool directories found.");
            return 0;
        }

        Map<String, List<PopulateMatch>> grouped = Populate.groupByCategory(matches);
        grouped.forEach((cat, items) -> {
            System.out.println("[" + cat + "]");
            items.forEach(m -> {
                String exes = m.foundExecutables().isEmpty() ? "" : "  (" + String.join(", ", m.foundExecutables()) + ")";
                System.out.println("  + " + m.path() + exes);
            });
        });

        if (!dryRun) {
            List<String> toAdd = matches.stream().map(PopulateMatch::path).toList();
            List<String> newUser = new ArrayList<>(current.userPath());
            newUser.addAll(toAdd);
            Snapshot updated = new Snapshot(current.systemPath(), newUser,
                current.systemPathRaw(), PathWriters.joinPath(newUser, osName),
                current.systemEnvVars(), current.userEnvVars());
            BackupStore.create(current, AppDirs.backupsHome(), "auto", "pre-populate", false);
            PathWriters.writeChanged(PathWriters.create(parent.varName), current, updated, Scope.USER);
            System.out.println("Added " + toAdd.size() + " director" + (toAdd.size() == 1 ? "y" : "ies") + " to PATH.");
        } else {
            System.out.println("[dry-run] No changes written.");
        }
        return 0;
    }
}
