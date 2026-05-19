package com.matthewdeanmartin.pathkeeper.diff;

import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;

import java.util.*;

public record PathDiff(List<String> added, List<String> removed, List<String> reordered) {

    public static PathDiff compute(List<String> original, List<String> updated, String osName) {
        List<String> origKeys = original.stream().map(e -> Diagnostics.canonicalizeEntry(e, osName)).toList();
        List<String> upKeys   = updated.stream().map(e -> Diagnostics.canonicalizeEntry(e, osName)).toList();

        Set<String> origSet = new HashSet<>(origKeys);
        Set<String> upSet   = new HashSet<>(upKeys);

        List<String> added = new ArrayList<>();
        for (int i = 0; i < updated.size(); i++) {
            if (!origSet.contains(upKeys.get(i))) added.add(updated.get(i));
        }

        List<String> removed = new ArrayList<>();
        for (int i = 0; i < original.size(); i++) {
            if (!upSet.contains(origKeys.get(i))) removed.add(original.get(i));
        }

        Map<String, Integer> origIndex = new LinkedHashMap<>();
        for (int i = 0; i < origKeys.size(); i++) {
            origIndex.putIfAbsent(origKeys.get(i), i);
        }
        List<String> reordered = new ArrayList<>();
        for (int i = 0; i < updated.size(); i++) {
            Integer origIdx = origIndex.get(upKeys.get(i));
            if (origIdx != null && origIdx != i) reordered.add(updated.get(i));
        }

        return new PathDiff(added, removed, reordered);
    }

    public boolean isEmpty() {
        return added.isEmpty() && removed.isEmpty() && reordered.isEmpty();
    }

    public String render() {
        if (isEmpty()) return "No changes.";
        StringBuilder sb = new StringBuilder();
        if (!added.isEmpty()) {
            sb.append("Added:\n");
            added.forEach(e -> sb.append("  + ").append(e).append('\n'));
        }
        if (!removed.isEmpty()) {
            sb.append("Removed:\n");
            removed.forEach(e -> sb.append("  - ").append(e).append('\n'));
        }
        if (!reordered.isEmpty()) {
            sb.append("Reordered:\n");
            reordered.forEach(e -> sb.append("  ~ ").append(e).append('\n'));
        }
        return sb.toString().stripTrailing();
    }
}
