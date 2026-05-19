package com.matthewdeanmartin.pathkeeper.selfcheck;

import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import com.matthewdeanmartin.pathkeeper.config.ConfigLoader;
import com.matthewdeanmartin.pathkeeper.platform.PathReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Selfcheck {

    private Selfcheck() {}

    public static List<SelfcheckResult> run(PathReader reader) {
        List<SelfcheckResult> results = new ArrayList<>();

        // 1. Backup directory exists and is writable
        Path backupDir = AppDirs.backupsHome();
        if (!Files.isDirectory(backupDir)) {
            results.add(SelfcheckResult.fail("Backup directory",
                backupDir + " does not exist",
                "Run `pathkeeper backup` once to create it."));
        } else if (!isWritable(backupDir)) {
            results.add(SelfcheckResult.fail("Backup directory",
                backupDir + " is not writable",
                "Check file system permissions on your pathkeeper directory."));
        } else {
            results.add(SelfcheckResult.pass("Backup directory", backupDir.toString()));
        }

        // 2. Config is readable
        try {
            ConfigLoader.load();
            results.add(SelfcheckResult.pass("Config file", AppDirs.configFile().toString()));
        } catch (IOException e) {
            results.add(SelfcheckResult.fail("Config file",
                "Could not load config: " + e.getMessage(),
                "Delete or repair your config.toml."));
        }

        // 3. Platform adapter can read PATH
        try {
            Snapshot snapshot = reader.readSnapshot();
            int total = snapshot.systemPath().size() + snapshot.userPath().size();
            results.add(SelfcheckResult.pass("Platform adapter", "read " + total + " PATH entries"));
        } catch (IOException e) {
            results.add(SelfcheckResult.fail("Platform adapter",
                "Could not read PATH: " + e.getMessage(),
                "Check platform adapter permissions (elevated shell may be needed on Windows)."));
        }

        // 4. Auto-backup configured (stub — schedule/shell-startup not yet implemented)
        results.add(SelfcheckResult.warn("Auto-backup",
            "no schedule or shell-startup hook detected",
            "Run `pathkeeper schedule install` or `pathkeeper shell-startup` to enable automatic backups."));

        return results;
    }

    public static boolean allPassed(List<SelfcheckResult> results) {
        return results.stream().allMatch(r -> SelfcheckResult.PASS.equals(r.status()));
    }

    private static boolean isWritable(Path path) {
        try {
            Path tmp = Files.createTempFile(path, ".pathkeeper-write-test-", null);
            Files.delete(tmp);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
