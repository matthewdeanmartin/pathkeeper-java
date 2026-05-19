package com.matthewdeanmartin.pathkeeper.splitlong;

import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;

import java.util.*;

public final class SplitLong {

    public static final int DEFAULT_MAX_LENGTH   = 2047;
    public static final int DEFAULT_CHUNK_LENGTH = 1800;

    private SplitLong() {}

    public static SplitLongPlan build(
        Snapshot snap,
        Scope scope,
        String osName,
        int maxLength,
        int chunkLength,
        String varPrefix
    ) throws IllegalArgumentException {
        if (!"windows".equals(osName)) throw new IllegalArgumentException("split-long is only supported on Windows");
        if (scope == Scope.ALL) throw new IllegalArgumentException("split-long requires --scope system or --scope user");

        String prefix = (varPrefix == null || varPrefix.isBlank())
            ? (scope == Scope.SYSTEM ? "PK_SYSTEM_PATHS" : "PK_USER_PATHS")
            : varPrefix.toUpperCase();

        List<String> entries = (scope == Scope.SYSTEM) ? snap.systemPath() : snap.userPath();
        String rawPath       = (scope == Scope.SYSTEM) ? snap.systemPathRaw() : snap.userPathRaw();

        if (rawPath.length() <= maxLength) {
            return new SplitLongPlan(scope, List.copyOf(entries), List.copyOf(entries),
                rawPath, rawPath, Map.of(), prefix, maxLength, chunkLength, false);
        }

        List<List<String>> chunks = chunkEntries(entries, chunkLength);
        Map<String, String> helperVars = new LinkedHashMap<>();
        List<String> pathRefs = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String varName = prefix + "_" + (i + 1);
            helperVars.put(varName, String.join(";", chunks.get(i)));
            pathRefs.add("%" + varName + "%");
        }

        String updatedRaw = String.join(";", pathRefs);
        return new SplitLongPlan(scope, List.copyOf(entries), List.copyOf(pathRefs),
            rawPath, updatedRaw, Map.copyOf(helperVars), prefix, maxLength, chunkLength,
            !rawPath.equals(updatedRaw));
    }

    public static String renderPlan(SplitLongPlan plan) {
        if (!plan.changed()) {
            return String.format("PATH length (%d) is within the %d-character limit. Nothing to do.%n",
                plan.originalRaw().length(), plan.maxLength());
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("PATH length: %d  (limit: %d)%n",
            plan.originalRaw().length(), plan.maxLength()));
        sb.append(String.format("Creating %d helper variable(s):%n", plan.helperVars().size()));
        plan.helperVars().forEach((name, value) -> {
            String preview = value.length() > 60 ? value.substring(0, 57) + "..." : value;
            sb.append(String.format("  %s = %s%n", name, preview));
        });
        sb.append(String.format("New PATH (%d chars): %s%n",
            plan.updatedRaw().length(), plan.updatedRaw()));
        return sb.toString();
    }

    public static Snapshot applyToSnapshot(Snapshot snap, SplitLongPlan plan) {
        Map<String, String> sysVars = new LinkedHashMap<>(snap.systemEnvVars());
        Map<String, String> userVars = new LinkedHashMap<>(snap.userEnvVars());

        List<String> sysPath  = new ArrayList<>(snap.systemPath());
        List<String> userPath = new ArrayList<>(snap.userPath());
        String sysRaw  = snap.systemPathRaw();
        String userRaw = snap.userPathRaw();

        if (plan.scope() == Scope.SYSTEM) {
            sysPath = new ArrayList<>(plan.updatedEntries());
            sysRaw  = plan.updatedRaw();
            sysVars.putAll(plan.helperVars());
        } else {
            userPath = new ArrayList<>(plan.updatedEntries());
            userRaw  = plan.updatedRaw();
            userVars.putAll(plan.helperVars());
        }

        return new Snapshot(List.copyOf(sysPath), List.copyOf(userPath),
            sysRaw, userRaw, Map.copyOf(sysVars), Map.copyOf(userVars));
    }

    private static List<List<String>> chunkEntries(List<String> entries, int maxChunk) {
        List<List<String>> chunks = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int currentLen = 0;

        for (String entry : entries) {
            int addedLen = entry.length() + (current.isEmpty() ? 0 : 1);
            if (!current.isEmpty() && currentLen + addedLen > maxChunk) {
                chunks.add(current);
                current = new ArrayList<>();
                currentLen = 0;
                addedLen = entry.length();
            }
            current.add(entry);
            currentLen += addedLen;
        }
        if (!current.isEmpty()) chunks.add(current);
        return chunks;
    }
}
