package com.matthewdeanmartin.pathkeeper.backup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BackupStoreTest {

    @TempDir Path backupDir;

    private Snapshot testSnapshot() {
        return new Snapshot(
            List.of("/usr/bin", "/usr/local/bin"),
            List.of("/home/user/bin"),
            "/usr/bin:/usr/local/bin",
            "/home/user/bin",
            Map.of(),
            Map.of()
        );
    }

    @Test
    void createWritesBackupFile() throws IOException {
        Snapshot snap = testSnapshot();
        Optional<Path> result = BackupStore.create(snap, backupDir, "manual", "test note", false);
        assertThat(result).isPresent();
        assertThat(result.get()).isRegularFile();
    }

    @Test
    void createSkipsWhenUnchanged() throws IOException {
        Snapshot snap = testSnapshot();
        BackupStore.create(snap, backupDir, "manual", null, false);
        Optional<Path> second = BackupStore.create(snap, backupDir, "manual", null, false);
        assertThat(second).isEmpty();
    }

    @Test
    void createForceWritesDespiteNoChange() throws IOException {
        Snapshot snap = testSnapshot();
        BackupStore.create(snap, backupDir, "manual", null, false);
        Optional<Path> forced = BackupStore.create(snap, backupDir, "manual", null, true);
        assertThat(forced).isPresent();
    }

    @Test
    void listReturnsNewestFirst() throws IOException {
        Snapshot snap = testSnapshot();
        BackupStore.create(snap, backupDir, "auto", null, true);
        Snapshot snap2 = new Snapshot(
            List.of("/usr/bin"),
            List.of("/home/user/bin"),
            "/usr/bin",
            "/home/user/bin",
            Map.of(),
            Map.of()
        );
        BackupStore.create(snap2, backupDir, "manual", null, false);

        List<BackupRecord> records = BackupStore.list(backupDir);
        assertThat(records).hasSize(2);
        // newest first — last written (manual) should be first
        assertThat(records.get(0).tag).isEqualTo("manual");
    }

    @Test
    void resolveByIndexWorks() throws IOException {
        Snapshot snap = testSnapshot();
        BackupStore.create(snap, backupDir, "auto", null, true);
        Snapshot snap2 = new Snapshot(
            List.of("/usr/bin"),
            List.of(),
            "/usr/bin",
            "",
            Map.of(),
            Map.of()
        );
        BackupStore.create(snap2, backupDir, "manual", null, false);

        BackupRecord first = BackupStore.resolve("1", backupDir);
        assertThat(first).isNotNull();
    }

    @Test
    void resolveLatestWhenBlank() throws IOException {
        Snapshot snap = testSnapshot();
        BackupStore.create(snap, backupDir, "manual", null, true);
        BackupRecord record = BackupStore.resolve("", backupDir);
        assertThat(record).isNotNull();
    }

    @Test
    void loadRoundTrips() throws IOException {
        Snapshot snap = testSnapshot();
        Optional<Path> written = BackupStore.create(snap, backupDir, "manual", "my note", false);
        BackupRecord loaded = BackupStore.load(written.get());

        assertThat(loaded.tag).isEqualTo("manual");
        assertThat(loaded.note).isEqualTo("my note");
        assertThat(loaded.systemPath).containsExactlyElementsOf(snap.systemPath());
        assertThat(loaded.userPath).containsExactlyElementsOf(snap.userPath());
    }

    @Test
    void contentHashIsStable() throws IOException {
        Snapshot snap = testSnapshot();
        Optional<Path> written = BackupStore.create(snap, backupDir, "manual", null, false);
        BackupRecord record = BackupStore.load(written.get());

        String h1 = BackupStore.contentHash(record);
        String h2 = BackupStore.contentHash(record);
        assertThat(h1).isEqualTo(h2).hasSize(12);
    }
}
