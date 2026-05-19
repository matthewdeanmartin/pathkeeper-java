package com.matthewdeanmartin.pathkeeper.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppDirs {

    public static final String HOME_ENV = "PATHKEEPER_HOME";

    private AppDirs() {}

    public static Path appHome() {
        String override = System.getenv(HOME_ENV);
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        return Path.of(System.getProperty("user.home"), ".pathkeeper");
    }

    public static Path backupsHome() {
        return appHome().resolve("backups");
    }

    public static Path configFile() {
        return appHome().resolve("config.toml");
    }

    /** Create app home, backups dir, and seed a default config file if missing. */
    public static void ensureAppState() throws IOException {
        Path home = appHome();
        Path backups = backupsHome();
        Path config = configFile();

        Files.createDirectories(home);
        Files.createDirectories(backups);

        if (!Files.exists(config)) {
            String defaults = """
                [general]
                max_auto_backups = 10
                max_manual_backups = 10
                max_backups = 30
                """;
            Files.writeString(config, defaults);
        }
    }
}
