package com.matthewdeanmartin.pathkeeper.diagnostics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosticsTest {

    @TempDir Path tmp;

    @Test
    void validDirectoryIsRecognized() throws Exception {
        Path dir = tmp.resolve("bin");
        Files.createDirectory(dir);

        DiagnosticReport report = Diagnostics.analyzeSnapshot(
            List.of(), List.of(dir.toString()), "linux", Scope.ALL, dir.toString());

        assertThat(report.entries()).hasSize(1);
        DiagnosticEntry e = report.entries().get(0);
        assertThat(e.exists()).isTrue();
        assertThat(e.isDir()).isTrue();
        assertThat(e.isDuplicate()).isFalse();
        assertThat(e.isEmpty()).isFalse();
    }

    @Test
    void missingDirectoryIsInvalid() {
        DiagnosticReport report = Diagnostics.analyzeSnapshot(
            List.of(), List.of("/totally/missing/path"), "linux", Scope.ALL, "/totally/missing/path");

        DiagnosticEntry e = report.entries().get(0);
        assertThat(e.exists()).isFalse();
        assertThat(report.summary().invalid()).isEqualTo(1);
    }

    @Test
    void duplicateIsDetected() throws Exception {
        Path dir = tmp.resolve("bin");
        Files.createDirectory(dir);
        String p = dir.toString();

        DiagnosticReport report = Diagnostics.analyzeSnapshot(
            List.of(), List.of(p, p), "linux", Scope.ALL, p + ":" + p);

        assertThat(report.summary().duplicates()).isEqualTo(1);
        assertThat(report.entries().get(1).isDuplicate()).isTrue();
        assertThat(report.entries().get(1).duplicateOf()).isEqualTo(1); // 1-based index of first occurrence
    }

    @Test
    void emptyEntryIsRecognized() {
        DiagnosticReport report = Diagnostics.analyzeSnapshot(
            List.of(), List.of(""), "linux", Scope.ALL, "");

        assertThat(report.entries().get(0).isEmpty()).isTrue();
        assertThat(report.summary().empty()).isEqualTo(1);
    }

    @Test
    void doctorChecksReturnResults() throws Exception {
        Path dir = tmp.resolve("bin");
        Files.createDirectory(dir);
        DiagnosticReport report = Diagnostics.analyzeSnapshot(
            List.of(), List.of(dir.toString()), "linux", Scope.ALL, dir.toString());

        List<DoctorCheck> checks = Diagnostics.doctorChecks(report);
        assertThat(checks).isNotEmpty();
        assertThat(checks).allMatch(c ->
            List.of(DoctorCheck.PASS, DoctorCheck.FAIL, DoctorCheck.WARN).contains(c.status()));
    }

    @Test
    void canonicalizeRemovesTrailingSlash() {
        String canon = Diagnostics.canonicalizeEntry("/usr/bin/", "linux");
        assertThat(canon).isEqualTo("/usr/bin");
    }

    @Test
    void windowsCanonicalizeLowercases() {
        String canon = Diagnostics.canonicalizeEntry("C:\\Windows\\System32\\", "windows");
        assertThat(canon).isEqualTo("c:\\windows\\system32");
    }
}
