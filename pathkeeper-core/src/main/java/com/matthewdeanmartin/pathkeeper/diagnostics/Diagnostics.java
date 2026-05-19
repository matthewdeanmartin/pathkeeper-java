package com.matthewdeanmartin.pathkeeper.diagnostics;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class Diagnostics {

    private static final Pattern WINDOWS_VAR = Pattern.compile("%[^%]+%");
    private static final Pattern WINDOWS_VAR_CAPTURE = Pattern.compile("%([^%]+)%");
    private static final Pattern UNIX_VAR = Pattern.compile("\\$(?:\\{[^}]+\\}|[A-Za-z_][A-Za-z0-9_]*)");

    private Diagnostics() {}

    public static String normalizedOsName() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "darwin";
        return "linux";
    }

    public static String pathSeparator(String osName) {
        return "windows".equals(osName) ? ";" : ":";
    }

    public static String expandEntry(String entry, String osName) {
        String cleaned = entry.strip().replaceAll("^\"|\"$", "");
        if ("windows".equals(osName)) {
            cleaned = expandWindowsVars(cleaned);
        } else {
            cleaned = expandUnixVars(cleaned);
        }
        String home = System.getProperty("user.home", "");
        return cleaned.replace("~", home);
    }

    public static boolean hasUnexpandedVariables(String entry, String osName) {
        if ("windows".equals(osName)) {
            return WINDOWS_VAR.matcher(entry).find();
        }
        return UNIX_VAR.matcher(entry).find();
    }

    public static String canonicalizeEntry(String entry, String osName) {
        String value = expandEntry(entry, osName);
        if ("windows".equals(osName)) {
            String normalized = value.replace("/", "\\").replaceAll("\\\\+$", "")
                .replaceAll("^\"|\"$", "").strip();
            return normalized.toLowerCase();
        }
        String normalized = value.replaceAll("/+$", "").strip();
        if ("darwin".equals(osName)) return normalized.toLowerCase();
        return normalized;
    }

    public static String rawForScope(Scope scope, String systemRaw, String userRaw, String osName) {
        return switch (scope) {
            case SYSTEM -> systemRaw;
            case USER   -> userRaw;
            case ALL    -> {
                String sep = pathSeparator(osName);
                if (!systemRaw.isEmpty() && !userRaw.isEmpty()) yield systemRaw + sep + userRaw;
                yield systemRaw.isEmpty() ? userRaw : systemRaw;
            }
        };
    }

    public static List<String> entriesForScope(Scope scope, List<String> system, List<String> user) {
        return switch (scope) {
            case SYSTEM -> system;
            case USER   -> user;
            case ALL    -> {
                List<String> combined = new ArrayList<>(system.size() + user.size());
                combined.addAll(system);
                combined.addAll(user);
                yield combined;
            }
        };
    }

    public static DiagnosticReport analyzeSnapshot(
        List<String> systemEntries,
        List<String> userEntries,
        String osName,
        Scope scope,
        String rawValue
    ) {
        List<DiagnosticEntry> entries = new ArrayList<>();
        int nextIndex = 1;
        Map<String, Integer> seen = new LinkedHashMap<>();

        if (scope == Scope.SYSTEM || scope == Scope.ALL) {
            List<DiagnosticEntry> group = analyzeGroup(systemEntries, Scope.SYSTEM, osName, nextIndex, seen);
            entries.addAll(group);
            nextIndex += group.size();
        }
        if (scope == Scope.USER || scope == Scope.ALL) {
            entries.addAll(analyzeGroup(userEntries, Scope.USER, osName, nextIndex, seen));
        }

        List<String> warnings = new ArrayList<>();
        int pathLength = rawValue.length();
        if ("windows".equals(osName)) {
            if (pathLength > 32767) {
                warnings.add("PATH exceeds the Windows registry limit of 32767 characters.");
            } else if (pathLength > 2047) {
                warnings.add("PATH exceeds the legacy setx limit of 2047 characters. Consider `pathkeeper split-long`.");
            }
        }
        long missingSepCount = entries.stream().filter(DiagnosticEntry::likelyMissingSeparator).count();
        if (missingSepCount > 0) {
            warnings.add("Some entries look like paths with a missing separator.");
        }

        DiagnosticSummary summary = new DiagnosticSummary(
            entries.size(),
            (int) entries.stream().filter(e -> e.exists() && e.isDir() && !e.isEmpty()).count(),
            (int) entries.stream().filter(e -> !e.value().isEmpty() && (!e.exists() || !e.isDir())).count(),
            (int) entries.stream().filter(DiagnosticEntry::isDuplicate).count(),
            (int) entries.stream().filter(DiagnosticEntry::isEmpty).count(),
            (int) entries.stream().filter(e -> e.exists() && !e.isDir()).count(),
            (int) missingSepCount,
            warnings
        );

        return new DiagnosticReport(entries, summary, osName, pathLength);
    }

    public static List<DoctorCheck> doctorChecks(DiagnosticReport report) {
        List<DoctorCheck> checks = new ArrayList<>();
        DiagnosticSummary s = report.summary();

        List<DiagnosticEntry> dups = filter(report.entries(), DiagnosticEntry::isDuplicate);
        checks.add(dups.isEmpty()
            ? DoctorCheck.pass("Duplicate entries", "none found")
            : DoctorCheck.fail("Duplicate entries", dups.size() + " found", dups, "Run `pathkeeper dedupe` to remove duplicates."));

        List<DiagnosticEntry> invalid = filter(report.entries(),
            e -> !e.value().isEmpty() && !e.exists() && !e.isDir() && !e.isEmpty() && !e.likelyMissingSeparator() && !e.isDuplicate());
        checks.add(invalid.isEmpty()
            ? DoctorCheck.pass("Missing directories", "none found")
            : DoctorCheck.fail("Missing directories", invalid.size() + " found", invalid,
                "Run `pathkeeper dedupe --remove-invalid` to remove, or `pathkeeper edit --remove <path>` for individual entries."));

        List<DiagnosticEntry> files = filter(report.entries(), e -> e.exists() && !e.isDir() && !e.isEmpty());
        checks.add(files.isEmpty()
            ? DoctorCheck.pass("Files in PATH (not directories)", "none found")
            : DoctorCheck.fail("Files in PATH (not directories)", files.size() + " found", files,
                "Remove with `pathkeeper edit --remove <path>`."));

        List<DiagnosticEntry> empties = filter(report.entries(), DiagnosticEntry::isEmpty);
        checks.add(empties.isEmpty()
            ? DoctorCheck.pass("Empty entries (stray separators)", "none found")
            : DoctorCheck.warn("Empty entries (stray separators)", s.empty() + " found", empties,
                "Run `pathkeeper dedupe` to clean them up."));

        List<DiagnosticEntry> glued = filter(report.entries(), DiagnosticEntry::likelyMissingSeparator);
        checks.add(glued.isEmpty()
            ? DoctorCheck.pass("Missing separators (glued paths)", "none found")
            : DoctorCheck.fail("Missing separators (glued paths)", s.missingSeparators() + " found", glued,
                "Use `pathkeeper edit` to split into separate entries."));

        List<DiagnosticEntry> unexp = filter(report.entries(),
            e -> e.hasUnexpandedVars() && !e.isEmpty() && !e.isDuplicate());
        checks.add(unexp.isEmpty()
            ? DoctorCheck.pass("Unresolvable variables", "none found")
            : DoctorCheck.warn("Unresolvable variables", unexp.size() + " found", unexp,
                "These entries contain %VAR% or $VAR references that cannot be expanded."));

        if ("windows".equals(report.osName())) {
            int len = report.pathLength();
            if (len > 32767) {
                checks.add(DoctorCheck.fail("PATH length", len + " chars (exceeds registry limit)", List.of(),
                    "Use `pathkeeper split-long`."));
            } else if (len > 2047) {
                checks.add(DoctorCheck.warn("PATH length", len + " chars (exceeds setx limit)", List.of(),
                    "Use `pathkeeper split-long`."));
            } else {
                checks.add(DoctorCheck.pass("PATH length", len + " chars"));
            }

            if (len == 1023 || len == 1024) {
                checks.add(DoctorCheck.warn("setx truncation sentinel",
                    len + " chars — classic setx/cmd truncation length", List.of(),
                    "Compare with a backup: `pathkeeper diff-current`."));
            } else {
                checks.add(DoctorCheck.pass("setx truncation sentinel", "no truncation at " + len + " chars"));
            }
        }

        return checks;
    }

    public static String explainEntry(DiagnosticEntry entry, String osName) {
        if (entry.isEmpty()) return "Empty PATH entry (stray separator). Run `pathkeeper dedupe` to clean up.";
        if (entry.isDuplicate() && entry.duplicateOf() != 0)
            return "Duplicate of #" + entry.duplicateOf() + ". Run `pathkeeper dedupe` to remove duplicates.";
        if (entry.hasUnexpandedVars())
            return "Contains an unexpanded variable (" + entry.value() + "). The variable may not be set.";
        if (entry.likelyMissingSeparator())
            return "Looks like two paths glued together: " + entry.value() + ". Use `pathkeeper edit` to split.";
        if (!entry.exists())
            return "Directory does not exist: " + entry.expandedValue() + ". Consider removing with `pathkeeper dedupe --remove-invalid`.";
        if (entry.exists() && !entry.isDir())
            return entry.expandedValue() + " is a file, not a directory. Remove with `pathkeeper edit --remove`.";
        return "This entry looks healthy.";
    }

    /** List executables in a directory (up to 30). */
    public static List<String> listExecutables(String directory, String osName) {
        File dir = new File(directory);
        if (!dir.isDirectory()) return List.of();

        Set<String> winExts = Set.of(".exe", ".cmd", ".bat", ".com", ".ps1");
        List<String> names = new ArrayList<>();

        File[] files = dir.listFiles();
        if (files == null) return List.of();

        for (File f : files) {
            if (names.size() >= 30) break;
            if (f.isDirectory()) continue;
            if ("windows".equals(osName)) {
                String ext = extension(f.getName()).toLowerCase();
                if (winExts.contains(ext)) {
                    String stem = f.getName().substring(0, f.getName().length() - ext.length());
                    names.add(stem);
                }
            } else {
                if (f.canExecute()) names.add(f.getName());
            }
        }

        names.sort(String.CASE_INSENSITIVE_ORDER);
        // dedupe case-insensitively
        List<String> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String n : names) {
            if (seen.add(n.toLowerCase())) result.add(n);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static List<DiagnosticEntry> analyzeGroup(
        List<String> entries,
        Scope scope,
        String osName,
        int startIndex,
        Map<String, Integer> seen
    ) {
        List<DiagnosticEntry> results = new ArrayList<>();
        for (int offset = 0; offset < entries.size(); offset++) {
            String entry = entries.get(offset);
            String expanded = expandEntry(entry, osName);
            String canonical = canonicalizeEntry(entry, osName);
            boolean isEmpty = entry.strip().isEmpty();

            boolean exists = false;
            boolean isDir = false;
            if (!expanded.isEmpty() && !isEmpty) {
                File f = new File(expanded);
                exists = f.exists();
                isDir = f.isDirectory();
            }

            int dupOf = 0;
            boolean isDup = false;
            if (!canonical.isEmpty()) {
                if (seen.containsKey(canonical)) {
                    isDup = true;
                    dupOf = seen.get(canonical);
                } else {
                    seen.put(canonical, startIndex + offset);
                }
            }

            List<String> exes = (isDir && !isEmpty) ? listExecutables(expanded, osName) : List.of();
            boolean missingSep = !isEmpty && !exists && looksLikeMissingSeparator(entry, osName);

            results.add(new DiagnosticEntry(
                startIndex + offset,
                entry,
                scope,
                exists,
                isDir,
                isDup,
                dupOf,
                isEmpty,
                hasUnexpandedVariables(expanded, osName),
                expanded,
                exes,
                missingSep
            ));
        }
        return results;
    }

    private static boolean looksLikeMissingSeparator(String entry, String osName) {
        if (entry.length() < 4) return false;
        if ("windows".equals(osName)) {
            for (int i = 2; i < entry.length() - 1; i++) {
                if (entry.charAt(i) == ':') {
                    char prev = entry.charAt(i - 1);
                    if (Character.isLetter(prev)) return true;
                }
            }
            return entry.substring(1).contains(";");
        }
        return entry.contains(":");
    }

    private static String expandWindowsVars(String value) {
        Map<String, String> env = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        System.getenv().forEach(env::put);
        var matcher = WINDOWS_VAR_CAPTURE.matcher(value);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = env.getOrDefault(key, matcher.group(0));
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String expandUnixVars(String value) {
        return value; // env vars not expanded on Unix (shell does it)
    }

    private static <T> List<T> filter(List<T> list, java.util.function.Predicate<T> pred) {
        return list.stream().filter(pred).toList();
    }

    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}
