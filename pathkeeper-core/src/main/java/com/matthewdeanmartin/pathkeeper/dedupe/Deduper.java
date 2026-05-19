package com.matthewdeanmartin.pathkeeper.dedupe;

import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;

import java.io.File;
import java.util.*;

public final class Deduper {

    private Deduper() {}

    /**
     * Remove duplicates (and optionally invalid entries) from entries.
     *
     * @param entries      the PATH entries to clean
     * @param osName       "windows", "darwin", or "linux"
     * @param keep         "first" (default) or "last"
     * @param removeInvalid also remove entries whose directory doesn't exist
     * @param preSeen      canonical entries already counted from an earlier scope pass
     */
    public static DedupeResult dedupe(
        List<String> entries,
        String osName,
        String keep,
        boolean removeInvalid,
        Set<String> preSeen
    ) {
        if (preSeen == null) preSeen = new HashSet<>();

        List<String> working = new ArrayList<>(entries);
        if ("last".equals(keep)) Collections.reverse(working);

        Set<String> seen = new HashSet<>(preSeen);
        List<String> kept = new ArrayList<>();
        List<String> removedDups = new ArrayList<>();
        List<String> removedInv = new ArrayList<>();
        List<String> removedEmpty = new ArrayList<>();

        for (String entry : working) {
            if (entry.strip().isEmpty()) {
                removedEmpty.add(entry);
                continue;
            }
            String canonical = Diagnostics.canonicalizeEntry(entry, osName);
            if (seen.contains(canonical)) {
                removedDups.add(entry);
                continue;
            }
            if (removeInvalid && !isValidDirectory(entry, osName)) {
                removedInv.add(entry);
                continue;
            }
            kept.add(entry);
            seen.add(canonical);
        }

        if ("last".equals(keep)) {
            Collections.reverse(kept);
            Collections.reverse(removedDups);
            Collections.reverse(removedInv);
            Collections.reverse(removedEmpty);
        }

        return new DedupeResult(List.copyOf(entries), List.copyOf(kept),
            List.copyOf(removedDups), List.copyOf(removedInv), List.copyOf(removedEmpty));
    }

    private static boolean isValidDirectory(String entry, String osName) {
        String expanded = Diagnostics.expandEntry(entry, osName);
        File f = new File(expanded);
        return f.exists() && f.isDirectory();
    }
}
