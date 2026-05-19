package com.matthewdeanmartin.pathkeeper.shadow;

import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;

import java.util.*;

public final class Shadows {

    private Shadows() {}

    public static List<ShadowGroup> findShadows(
        List<String> systemEntries,
        List<String> userEntries,
        String osName,
        Scope scope
    ) {
        record DirEntry(String dir, Scope scope, int index) {}

        List<DirEntry> dirs = new ArrayList<>();
        Set<String> seenCanon = new LinkedHashSet<>();
        int idx = 1;

        if (scope == Scope.SYSTEM || scope == Scope.ALL) {
            for (String entry : systemEntries) {
                String canon = Diagnostics.canonicalizeEntry(entry, osName);
                if (!canon.isEmpty() && seenCanon.add(canon)) {
                    dirs.add(new DirEntry(entry, Scope.SYSTEM, idx));
                }
                idx++;
            }
        }
        if (scope == Scope.USER || scope == Scope.ALL) {
            for (String entry : userEntries) {
                String canon = Diagnostics.canonicalizeEntry(entry, osName);
                if (!canon.isEmpty() && seenCanon.add(canon)) {
                    dirs.add(new DirEntry(entry, Scope.USER, idx));
                }
                idx++;
            }
        }

        Map<String, List<ShadowEntry>> exeMap = new LinkedHashMap<>();
        for (DirEntry d : dirs) {
            for (String name : Diagnostics.listExecutables(d.dir(), osName)) {
                String key = ("windows".equals(osName) || "darwin".equals(osName))
                    ? name.toLowerCase() : name;
                exeMap.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new ShadowEntry(d.dir(), d.scope(), d.index()));
            }
        }

        List<ShadowGroup> groups = new ArrayList<>();
        for (var entry : exeMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                groups.add(new ShadowGroup(entry.getKey(), entry.getValue()));
            }
        }

        groups.sort(Comparator.comparing(ShadowGroup::name));
        return groups;
    }
}
