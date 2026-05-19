package com.matthewdeanmartin.pathkeeper.dedupe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DedupeTest {

    @TempDir Path tmp;

    @Test
    void removesDuplicates() {
        var r = Deduper.dedupe(List.of("/usr/bin", "/usr/local/bin", "/usr/bin"), "linux", "first", false, null);
        assertThat(r.cleaned()).containsExactly("/usr/bin", "/usr/local/bin");
        assertThat(r.removedDuplicates()).containsExactly("/usr/bin");
    }

    @Test
    void keepLastReversesPreference() {
        var r = Deduper.dedupe(List.of("/usr/bin", "/usr/local/bin", "/usr/bin"), "linux", "last", false, null);
        assertThat(r.cleaned()).containsExactly("/usr/local/bin", "/usr/bin");
    }

    @Test
    void removesEmptyEntries() {
        var r = Deduper.dedupe(List.of("/usr/bin", "", "/usr/local/bin"), "linux", "first", false, null);
        assertThat(r.cleaned()).containsExactly("/usr/bin", "/usr/local/bin");
        assertThat(r.removedEmpty()).hasSize(1);
    }

    @Test
    void removesInvalidWhenFlagSet() throws Exception {
        Path real = Files.createDirectory(tmp.resolve("real"));
        var r = Deduper.dedupe(List.of(real.toString(), "/does/not/exist"), "linux", "first", true, null);
        assertThat(r.cleaned()).containsExactly(real.toString());
        assertThat(r.removedInvalid()).containsExactly("/does/not/exist");
    }

    @Test
    void preSeenPreventsCarryOver() {
        Set<String> preSeen = Set.of("/usr/bin");
        var r = Deduper.dedupe(List.of("/usr/bin", "/usr/local/bin"), "linux", "first", false, preSeen);
        assertThat(r.cleaned()).containsExactly("/usr/local/bin");
        assertThat(r.removedDuplicates()).containsExactly("/usr/bin");
    }

    @Test
    void windowsCaseInsensitiveDedupe() {
        var r = Deduper.dedupe(
            List.of("C:\\Windows\\System32", "c:\\windows\\system32"),
            "windows", "first", false, null);
        assertThat(r.cleaned()).hasSize(1);
    }
}
