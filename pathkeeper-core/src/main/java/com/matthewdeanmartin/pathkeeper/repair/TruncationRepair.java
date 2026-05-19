package com.matthewdeanmartin.pathkeeper.repair;

import com.matthewdeanmartin.pathkeeper.backup.BackupRecord;
import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public final class TruncationRepair {

    private TruncationRepair() {}

    public static List<Repair> findTruncated(
        Snapshot snap,
        Scope scope,
        String osName,
        List<BackupRecord> records
    ) {
        List<Repair> repairs = new ArrayList<>();

        if (scope == Scope.SYSTEM || scope == Scope.ALL) {
            List<String> entries = snap.systemPath();
            for (int i = 0; i < entries.size(); i++) {
                List<RepairCandidate> candidates = candidatesFor(entries.get(i), osName, Scope.SYSTEM, records);
                if (!candidates.isEmpty()) {
                    repairs.add(new Repair(entries.get(i), i, Scope.SYSTEM, i + 1, candidates));
                }
            }
        }
        if (scope == Scope.USER || scope == Scope.ALL) {
            List<String> entries = snap.userPath();
            for (int i = 0; i < entries.size(); i++) {
                List<RepairCandidate> candidates = candidatesFor(entries.get(i), osName, Scope.USER, records);
                if (!candidates.isEmpty()) {
                    repairs.add(new Repair(entries.get(i), i, Scope.USER, i + 1, candidates));
                }
            }
        }
        return repairs;
    }

    private static List<RepairCandidate> candidatesFor(
        String entry, String osName, Scope scope, List<BackupRecord> records
    ) {
        String expanded = Diagnostics.expandEntry(entry, osName);
        if (new File(expanded).exists()) return List.of();
        if (!looksLikeTruncated(expanded, osName)) return List.of();

        List<String> targetParts = normalizeParts(expanded, osName);
        if (targetParts.isEmpty()) return List.of();

        Map<String, Boolean> seen = new LinkedHashMap<>();
        List<RepairCandidate> candidates = new ArrayList<>();

        for (BackupRecord rec : records) {
            List<String> entries = (scope == Scope.SYSTEM) ? rec.systemPath : rec.userPath;
            if (entries == null) continue;
            for (String e : entries) {
                String exp = Diagnostics.expandEntry(e, osName);
                String canonical = canonicalize(exp, osName);
                if (seen.containsKey(canonical)) continue;
                File f = new File(exp);
                if (!f.exists() || !f.isDirectory()) continue;
                if (matchesSuffix(exp, targetParts, osName)) {
                    seen.put(canonical, true);
                    candidates.add(new RepairCandidate(exp,
                        "backup " + Path.of(rec.sourcePath).getFileName()));
                }
            }
        }
        return candidates;
    }

    private static boolean looksLikeTruncated(String entry, String osName) {
        if ("windows".equals(osName)) {
            if (entry.length() >= 3 && entry.charAt(1) == ':' &&
                (entry.charAt(2) == '\\' || entry.charAt(2) == '/')) return false;
            return entry.contains("\\") || entry.contains("/");
        }
        return !entry.startsWith("/") && entry.contains("/");
    }

    private static List<String> normalizeParts(String path, String osName) {
        path = path.replace("\"", "");
        String sep = "windows".equals(osName) ? "\\\\" : "/";
        if ("windows".equals(osName)) path = path.replace("/", "\\");
        String[] parts = path.split(sep.equals("\\\\") ? "\\\\" : sep, -1);
        List<String> result = new ArrayList<>();
        for (String p : parts) {
            if (!p.isEmpty()) result.add("windows".equals(osName) ? p.toLowerCase() : p);
        }
        return result;
    }

    private static boolean matchesSuffix(String candidate, List<String> targetParts, String osName) {
        List<String> candidateParts = normalizeParts(candidate, osName);
        if (candidateParts.size() < targetParts.size()) return false;
        List<String> tail = candidateParts.subList(candidateParts.size() - targetParts.size(), candidateParts.size());
        for (int i = 0; i < tail.size(); i++) {
            if ("windows".equals(osName)) {
                if (!tail.get(i).equalsIgnoreCase(targetParts.get(i))) return false;
            } else {
                if (!tail.get(i).equals(targetParts.get(i))) return false;
            }
        }
        return true;
    }

    private static String canonicalize(String path, String osName) {
        return "windows".equals(osName) ? path.replace("/", "\\").toLowerCase() : path;
    }
}
