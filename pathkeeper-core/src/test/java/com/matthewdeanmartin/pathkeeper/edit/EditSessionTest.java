package com.matthewdeanmartin.pathkeeper.edit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EditSessionTest {

    @Test
    void addAppendsToEnd() {
        var s = new EditSession(List.of("/a", "/b"), "linux");
        s.add("/c");
        assertThat(s.entries()).containsExactly("/a", "/b", "/c");
    }

    @Test
    void addAtPosition() {
        var s = new EditSession(List.of("/a", "/b"), "linux");
        s.add("/x", 0);
        assertThat(s.entries()).containsExactly("/x", "/a", "/b");
    }

    @Test
    void deleteByIndex() {
        var s = new EditSession(List.of("/a", "/b", "/c"), "linux");
        s.delete(1);
        assertThat(s.entries()).containsExactly("/a", "/c");
    }

    @Test
    void deleteByValue() {
        var s = new EditSession(List.of("/a", "/b", "/c"), "linux");
        boolean found = s.deleteByValue("/b", "linux");
        assertThat(found).isTrue();
        assertThat(s.entries()).containsExactly("/a", "/c");
    }

    @Test
    void moveUp() {
        var s = new EditSession(List.of("/a", "/b", "/c"), "linux");
        s.moveUp(2);
        assertThat(s.entries()).containsExactly("/a", "/c", "/b");
    }

    @Test
    void moveDown() {
        var s = new EditSession(List.of("/a", "/b", "/c"), "linux");
        s.moveDown(0);
        assertThat(s.entries()).containsExactly("/b", "/a", "/c");
    }

    @Test
    void replaceByValue() {
        var s = new EditSession(List.of("/a", "/b"), "linux");
        boolean found = s.replaceByValue("/b", "/new");
        assertThat(found).isTrue();
        assertThat(s.entries()).containsExactly("/a", "/new");
    }

    @Test
    void undo() {
        var s = new EditSession(List.of("/a", "/b"), "linux");
        s.add("/c");
        s.undo();
        assertThat(s.entries()).containsExactly("/a", "/b");
    }

    @Test
    void reset() {
        var s = new EditSession(List.of("/a", "/b"), "linux");
        s.add("/c");
        s.reset();
        assertThat(s.entries()).containsExactly("/a", "/b");
    }

    @Test
    void diffReflectsChanges() {
        var s = new EditSession(List.of("/a", "/b"), "linux");
        s.add("/c");
        var diff = s.diff();
        assertThat(diff.added()).containsExactly("/c");
        assertThat(diff.removed()).isEmpty();
    }
}
