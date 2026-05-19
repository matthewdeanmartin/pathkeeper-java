package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.backup.BackupRecord;
import com.matthewdeanmartin.pathkeeper.backup.BackupStore;
import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "backups",
    description = "List and inspect existing backups.",
    mixinStandardHelpOptions = true,
    subcommands = {
        BackupsCommand.ListCommand.class,
        BackupsCommand.ShowCommand.class,
    }
)
public class BackupsCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.err.println("Specify a subcommand: list, show");
        return 1;
    }

    // -------------------------------------------------------------------------

    @Command(name = "list", description = "List all available backups.", mixinStandardHelpOptions = true)
    static class ListCommand implements Callable<Integer> {

        @ParentCommand BackupsCommand parent;

        @Option(names = {"--json"}, description = "Output as JSON")
        boolean json;

        private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

        @Override
        public Integer call() throws Exception {
            AppDirs.ensureAppState();
            List<BackupRecord> records = BackupStore.list(AppDirs.backupsHome());
            if (records.isEmpty()) {
                System.out.println("No backups found.");
                return 0;
            }
            int i = 1;
            for (BackupRecord r : records) {
                String ts = FMT.format(r.timestamp);
                String note = (r.note != null && !r.note.isBlank()) ? "  " + r.note : "";
                System.out.printf("%3d  %s  [%s]%s%n", i++, ts, r.tag, note);
            }
            return 0;
        }
    }

    // -------------------------------------------------------------------------

    @Command(name = "show", description = "Show the contents of a backup.", mixinStandardHelpOptions = true)
    static class ShowCommand implements Callable<Integer> {

        @ParentCommand BackupsCommand parent;

        @Parameters(index = "0", description = "Backup identifier (index, filename prefix, or path)", arity = "0..1", defaultValue = "")
        String identifier;

        @Option(names = {"--scope"}, description = "system, user, or all (default: all)", defaultValue = "all")
        String scope;

        @Override
        public Integer call() throws Exception {
            AppDirs.ensureAppState();
            Path backupDir = AppDirs.backupsHome();
            BackupRecord record = BackupStore.resolve(identifier, backupDir);

            System.out.println("Backup: " + Path.of(record.sourcePath).getFileName());
            System.out.println("Timestamp: " + record.timestamp);
            System.out.println("Tag: " + record.tag);
            if (record.note != null && !record.note.isBlank()) {
                System.out.println("Note: " + record.note);
            }
            System.out.println("OS: " + record.os);
            System.out.println();

            if (!scope.equals("user")) {
                System.out.println("System PATH (" + record.systemPath.size() + " entries):");
                record.systemPath.forEach(e -> System.out.println("  " + e));
            }
            if (!scope.equals("system")) {
                System.out.println("User PATH (" + record.userPath.size() + " entries):");
                record.userPath.forEach(e -> System.out.println("  " + e));
            }
            return 0;
        }
    }
}
