package com.matthewdeanmartin.pathkeeper.edit;

import com.matthewdeanmartin.pathkeeper.diff.PathDiff;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class EditSession {

    private final List<String> original;
    private List<String> current;
    private final Deque<List<String>> history = new ArrayDeque<>();
    private final String osName;

    public EditSession(List<String> entries, String osName) {
        this.original = List.copyOf(entries);
        this.current  = new ArrayList<>(entries);
        this.osName   = osName;
    }

    public List<String> entries() { return List.copyOf(current); }

    /** Append value at end, or insert at position (0-based). */
    public void add(String value, int position) {
        checkpoint();
        if (position < 0 || position >= current.size()) {
            current.add(value);
        } else {
            current.add(position, value);
        }
    }

    public void add(String value) { add(value, -1); }

    public void delete(int index) {
        checkpoint();
        current.remove(index);
    }

    /** Remove by value (first match). Returns true if found. */
    public boolean deleteByValue(String value, String osName) {
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).equalsIgnoreCase(value) ||
                com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics
                    .canonicalizeEntry(current.get(i), osName)
                    .equals(com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics
                        .canonicalizeEntry(value, osName))) {
                delete(i);
                return true;
            }
        }
        return false;
    }

    public void move(int fromIndex, int toIndex) {
        checkpoint();
        String item = current.remove(fromIndex);
        int target = Math.max(0, Math.min(toIndex, current.size()));
        current.add(target, item);
    }

    /** Move entry up one position (toward index 0). */
    public void moveUp(int index) {
        if (index > 0) move(index, index - 1);
    }

    /** Move entry down one position (toward end). */
    public void moveDown(int index) {
        if (index < current.size() - 1) move(index, index + 1);
    }

    public void replace(int index, String value) {
        checkpoint();
        current.set(index, value);
    }

    /** Replace old value with new value (first match). Returns true if found. */
    public boolean replaceByValue(String oldValue, String newValue) {
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).equals(oldValue)) {
                replace(i, newValue);
                return true;
            }
        }
        return false;
    }

    public void swap(int left, int right) {
        checkpoint();
        String tmp = current.get(left);
        current.set(left, current.get(right));
        current.set(right, tmp);
    }

    public boolean undo() {
        if (history.isEmpty()) return false;
        current = new ArrayList<>(history.pop());
        return true;
    }

    public void reset() {
        checkpoint();
        current = new ArrayList<>(original);
    }

    public PathDiff diff() {
        return PathDiff.compute(original, current, osName);
    }

    private void checkpoint() {
        history.push(new ArrayList<>(current));
    }
}
