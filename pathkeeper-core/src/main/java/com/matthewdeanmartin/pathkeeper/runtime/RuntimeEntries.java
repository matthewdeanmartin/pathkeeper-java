package com.matthewdeanmartin.pathkeeper.runtime;

import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;

import java.util.*;

public final class RuntimeEntries {

    private RuntimeEntries() {}

    public static List<RuntimeEntry> detect(
        List<String> systemPath,
        List<String> userPath,
        String osName
    ) {
        Set<String> persistedSystem = new HashSet<>();
        for (String e : systemPath) {
            if (!e.strip().isEmpty()) persistedSystem.add(Diagnostics.canonicalizeEntry(e, osName));
        }
        Set<String> persistedUser = new HashSet<>();
        for (String e : userPath) {
            if (!e.strip().isEmpty()) persistedUser.add(Diagnostics.canonicalizeEntry(e, osName));
        }

        String sep = Diagnostics.pathSeparator(osName);
        String livePath = System.getenv("PATH");
        if (livePath == null) return List.of();

        List<RuntimeEntry> results = new ArrayList<>();
        for (String entry : livePath.split(java.util.regex.Pattern.quote(sep), -1)) {
            if (entry.strip().isEmpty()) continue;
            String canon = Diagnostics.canonicalizeEntry(entry, osName);
            if (persistedSystem.contains(canon)) {
                results.add(new RuntimeEntry(entry, true, Scope.SYSTEM));
            } else if (persistedUser.contains(canon)) {
                results.add(new RuntimeEntry(entry, true, Scope.USER));
            } else {
                results.add(new RuntimeEntry(entry, false, null));
            }
        }
        return results;
    }
}
