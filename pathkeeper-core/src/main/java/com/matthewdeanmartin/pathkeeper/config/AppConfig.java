package com.matthewdeanmartin.pathkeeper.config;

public record AppConfig(
    int maxAutoBackups,
    int maxManualBackups,
    int maxBackups
) {
    public static AppConfig defaults() {
        return new AppConfig(10, 10, 30);
    }
}
