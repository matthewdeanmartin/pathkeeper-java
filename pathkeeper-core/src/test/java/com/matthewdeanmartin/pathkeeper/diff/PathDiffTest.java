package com.matthewdeanmartin.pathkeeper.diff;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PathDiffTest {

    @Test
    void noChanges() {
        PathDiff diff = PathDiff.compute(
            List.of("/usr/bin", "/usr/local/bin"),
            List.of("/usr/bin", "/usr/local/bin"),
            "linux");
        assertThat(diff.isEmpty()).isTrue();
        assertThat(diff.render()).isEqualTo("No changes.");
    }

    @Test
    void detectsAdded() {
        PathDiff diff = PathDiff.compute(
            List.of("/usr/bin"),
            List.of("/usr/bin", "/usr/local/bin"),
            "linux");
        assertThat(diff.added()).containsExactly("/usr/local/bin");
        assertThat(diff.removed()).isEmpty();
    }

    @Test
    void detectsRemoved() {
        PathDiff diff = PathDiff.compute(
            List.of("/usr/bin", "/usr/local/bin"),
            List.of("/usr/bin"),
            "linux");
        assertThat(diff.removed()).containsExactly("/usr/local/bin");
        assertThat(diff.added()).isEmpty();
    }

    @Test
    void detectsReordered() {
        PathDiff diff = PathDiff.compute(
            List.of("/usr/bin", "/usr/local/bin"),
            List.of("/usr/local/bin", "/usr/bin"),
            "linux");
        assertThat(diff.reordered()).isNotEmpty();
    }

    @Test
    void windowsCaseInsensitive() {
        PathDiff diff = PathDiff.compute(
            List.of("C:\\Windows\\System32"),
            List.of("c:\\windows\\system32"),
            "windows");
        assertThat(diff.isEmpty()).isTrue();
    }

    @Test
    void renderIncludesPlusMinus() {
        PathDiff diff = PathDiff.compute(
            List.of("/usr/bin"),
            List.of("/usr/local/bin"),
            "linux");
        String rendered = diff.render();
        assertThat(rendered).contains("+ /usr/local/bin");
        assertThat(rendered).contains("- /usr/bin");
    }
}
