package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.backup.BackupStore;
import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;
import com.matthewdeanmartin.pathkeeper.edit.EditSession;
import com.matthewdeanmartin.pathkeeper.platform.PathReaders;
import com.matthewdeanmartin.pathkeeper.writer.PathWriters;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "edit",
    description = "Non-interactively edit PATH entries (add, remove, move, replace).",
    mixinStandardHelpOptions = true
)
public class EditCommand implements Callable<Integer> {

    @ParentCommand PathkeeperCommand parent;

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

    @Option(names = {"--force"}, description = "Apply changes without prompting")
    boolean force;

    @Override
    public Integer call() throws Exception {
        AppDirs.ensureAppState();
        String osName = Diagnostics.normalizedOsName();
        Scope s = Scope.parse(scope);
        Snapshot current = PathReaders.create().readSnapshotVar(parent.varName);

        List<String> entries = Diagnostics.entriesForScope(s, current.systemPath(), current.userPath());
        EditSession session = new EditSession(entries, osName);

        if (add != null)    add.forEach(session::add);
        if (remove != null) remove.forEach(v -> session.deleteByValue(v, osName));
        if (moveUp != null) {
            for (int i = 0; i < session.entries().size(); i++) {
                if (session.entries().get(i).equalsIgnoreCase(moveUp)) { session.moveUp(i); break; }
            }
        }
        if (moveDown != null) {
            for (int i = 0; i < session.entries().size(); i++) {
                if (session.entries().get(i).equalsIgnoreCase(moveDown)) { session.moveDown(i); break; }
            }
        }
        if (replace != null) {
            String[] parts = replace.split("=", 2);
            if (parts.length == 2) session.replaceByValue(parts[0], parts[1]);
            else { System.err.println("--replace requires OLD=NEW format"); return 1; }
        }

        var diff = session.diff();
        System.out.println(diff.isEmpty() ? "No changes." : diff.render());

        if (!dryRun && !diff.isEmpty()) {
            BackupStore.create(current, AppDirs.backupsHome(), "auto", "pre-edit", false);
            List<String> updated = session.entries();
            Snapshot newSnap = buildSnapshot(current, updated, s, osName);
            PathWriters.writeChanged(PathWriters.create(parent.varName), current, newSnap, s);
            System.out.println("PATH updated.");
        } else if (dryRun) {
            System.out.println("[dry-run] No changes written.");
        }
        return 0;
    }

    private Snapshot buildSnapshot(Snapshot current, List<String> updated, Scope s, String osName) {
        List<String> newSystem = s == Scope.USER ? current.systemPath() : updated;
        List<String> newUser   = s == Scope.SYSTEM ? current.userPath() : updated;
        return new Snapshot(newSystem, newUser,
            PathWriters.joinPath(newSystem, osName), PathWriters.joinPath(newUser, osName),
            current.systemEnvVars(), current.userEnvVars());
    }
}
