package com.matthewdeanmartin.pathkeeper.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AppDirsTest {

    @TempDir Path tmp;

    @Test
    void ensureAppStateCreatesDirectoriesAndConfig() throws IOException {
        System.setProperty("user.home", tmp.toString());
        // Unset any override so the default path kicks in
        // (PATHKEEPER_HOME env is read-only; we work via user.home instead)

        AppDirs.ensureAppState();

        assertThat(AppDirs.appHome()).isDirectory();
        assertThat(AppDirs.backupsHome()).isDirectory();
        assertThat(AppDirs.configFile()).isRegularFile();
    }

    @Test
    void ensureAppStateIsIdempotent() throws IOException {
        System.setProperty("user.home", tmp.toString());

        AppDirs.ensureAppState();
        AppDirs.ensureAppState(); // second call must not throw

        assertThat(AppDirs.configFile()).isRegularFile();
    }
}
