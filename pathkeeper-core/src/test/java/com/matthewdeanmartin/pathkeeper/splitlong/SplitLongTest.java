package com.matthewdeanmartin.pathkeeper.splitlong;

import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SplitLongTest {

    private Snapshot snapshotWithUserPath(String rawPath) {
        List<String> entries = List.of(rawPath.split(";", -1));
        return new Snapshot(List.of(), entries, "", rawPath, Map.of(), Map.of());
    }

    @Test
    void noChangeWhenWithinLimit() throws Exception {
        String raw = "C:\\foo;C:\\bar";
        Snapshot snap = snapshotWithUserPath(raw);
        SplitLongPlan plan = SplitLong.build(snap, Scope.USER, "windows", 2047, 1800, null);
        assertThat(plan.changed()).isFalse();
    }

    @Test
    void splitsWhenOverLimit() {
        // Build a path that exceeds the 50-char limit
        String raw = "C:\\a;C:\\b;C:\\c;C:\\d;C:\\e";
        Snapshot snap = snapshotWithUserPath(raw);
        SplitLongPlan plan = SplitLong.build(snap, Scope.USER, "windows", 10, 8, null);
        assertThat(plan.changed()).isTrue();
        assertThat(plan.helperVars()).isNotEmpty();
        assertThat(plan.updatedRaw()).contains("%PK_USER_PATHS_1%");
    }

    @Test
    void rejectsNonWindowsOs() {
        Snapshot snap = new Snapshot(List.of(), List.of(), "", "", Map.of(), Map.of());
        assertThatThrownBy(() -> SplitLong.build(snap, Scope.USER, "linux", 2047, 1800, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAllScope() {
        Snapshot snap = new Snapshot(List.of(), List.of(), "", "", Map.of(), Map.of());
        assertThatThrownBy(() -> SplitLong.build(snap, Scope.ALL, "windows", 2047, 1800, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applyToSnapshotUpdatesUserPath() {
        String raw = "C:\\a;C:\\b;C:\\c;C:\\d;C:\\e";
        Snapshot snap = snapshotWithUserPath(raw);
        SplitLongPlan plan = SplitLong.build(snap, Scope.USER, "windows", 10, 8, null);
        if (plan.changed()) {
            Snapshot updated = SplitLong.applyToSnapshot(snap, plan);
            assertThat(updated.userPath()).isEqualTo(plan.updatedEntries());
            assertThat(updated.userEnvVars()).containsAllEntriesOf(plan.helperVars());
        }
    }
}
