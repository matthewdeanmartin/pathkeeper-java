package com.matthewdeanmartin.pathkeeper.config;

import com.moandjiezana.toml.Toml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigLoader {

    private ConfigLoader() {}

    public static AppConfig load() throws IOException {
        Path configFile = AppDirs.configFile();
        if (!Files.exists(configFile)) {
            return AppConfig.defaults();
        }

        Toml toml = new Toml().read(configFile.toFile());
        Toml general = toml.getTable("general");
        if (general == null) {
            return AppConfig.defaults();
        }

        AppConfig defaults = AppConfig.defaults();
        int maxAuto    = Math.toIntExact(general.getLong("max_auto_backups",    (long) defaults.maxAutoBackups()));
        int maxManual  = Math.toIntExact(general.getLong("max_manual_backups",  (long) defaults.maxManualBackups()));
        int maxBackups = Math.toIntExact(general.getLong("max_backups",         (long) defaults.maxBackups()));

        return new AppConfig(maxAuto, maxManual, maxBackups);
    }
}
