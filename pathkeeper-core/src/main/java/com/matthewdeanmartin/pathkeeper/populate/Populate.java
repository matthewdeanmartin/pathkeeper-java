package com.matthewdeanmartin.pathkeeper.populate;

import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class Populate {

    private static final String BUNDLED_CATALOG = "/com/matthewdeanmartin/pathkeeper/populate/known_tools.toml";

    private Populate() {}

    /** Load catalog from user's app dir, seeding from the bundled resource on first use. */
    public static List<CatalogTool> loadCatalog() throws IOException {
        Path catalogPath = AppDirs.appHome().resolve("known_tools.toml");
        if (!Files.exists(catalogPath)) {
            try (InputStream in = Populate.class.getResourceAsStream(BUNDLED_CATALOG)) {
                if (in == null) throw new IOException("Bundled catalog not found on classpath");
                Files.copy(in, catalogPath);
            }
        }
        return parseCatalog(Files.readString(catalogPath));
    }

    /** Discover tool directories that exist on disk but are not in existingEntries. */
    public static List<PopulateMatch> discover(
        List<CatalogTool> tools,
        List<String> existingEntries,
        String osName,
        String category
    ) {
        Set<String> existing = new HashSet<>();
        for (String e : existingEntries) existing.add(Diagnostics.canonicalizeEntry(e, osName));

        Set<String> seen = new LinkedHashSet<>();
        List<PopulateMatch> matches = new ArrayList<>();

        for (CatalogTool tool : tools) {
            if (tool.os != null && !tool.os.equals("all") && !tool.os.equals(osName)) continue;
            if (category != null && !category.isBlank() &&
                !category.equalsIgnoreCase(tool.category)) continue;
            if (tool.patterns == null) continue;

            for (String pattern : tool.patterns) {
                String expanded = expandPattern(pattern);
                List<Path> candidates = glob(expanded);
                for (Path candidate : candidates) {
                    if (!Files.isDirectory(candidate)) continue;
                    String canonical = Diagnostics.canonicalizeEntry(candidate.toString(), osName);
                    if (existing.contains(canonical) || !seen.add(canonical)) continue;
                    matches.add(new PopulateMatch(tool.name, tool.category,
                        candidate.toString(), listExecutables(candidate.toFile(), osName)));
                }
            }
        }

        matches.sort(Comparator.comparing((PopulateMatch m) -> m.category().toLowerCase())
            .thenComparing(m -> m.path().toLowerCase()));
        return matches;
    }

    public static Map<String, List<PopulateMatch>> groupByCategory(List<PopulateMatch> matches) {
        Map<String, List<PopulateMatch>> grouped = new LinkedHashMap<>();
        for (PopulateMatch m : matches) grouped.computeIfAbsent(m.category(), k -> new ArrayList<>()).add(m);
        return grouped;
    }

    // -------------------------------------------------------------------------

    private static List<CatalogTool> parseCatalog(String tomlContent) {
        List<CatalogTool> tools = new ArrayList<>();
        CatalogTool current = null;
        String pendingArrayKey = null;
        List<String> pendingArrayValues = new ArrayList<>();

        for (String rawLine : tomlContent.split("\\R")) {
            String line = stripComments(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }
            if ("[[tools]]".equals(line)) {
                if (current != null) {
                    tools.add(normalizeTool(current));
                }
                current = new CatalogTool();
                pendingArrayKey = null;
                pendingArrayValues = new ArrayList<>();
                continue;
            }
            if (current == null) {
                continue;
            }

            if (pendingArrayKey != null) {
                pendingArrayValues.addAll(parseArrayItems(line));
                if (line.contains("]")) {
                    assignArray(current, pendingArrayKey, pendingArrayValues);
                    pendingArrayKey = null;
                    pendingArrayValues = new ArrayList<>();
                }
                continue;
            }

            int eq = line.indexOf('=');
            if (eq < 0) {
                continue;
            }

            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            if (value.startsWith("[")) {
                pendingArrayKey = key;
                pendingArrayValues.addAll(parseArrayItems(value.substring(1)));
                if (value.contains("]")) {
                    assignArray(current, pendingArrayKey, pendingArrayValues);
                    pendingArrayKey = null;
                    pendingArrayValues = new ArrayList<>();
                }
                continue;
            }

            assignScalar(current, key, parseString(value));
        }

        if (current != null) {
            if (pendingArrayKey != null) {
                assignArray(current, pendingArrayKey, pendingArrayValues);
            }
            tools.add(normalizeTool(current));
        }
        return tools.stream().filter(tool -> tool.name != null && !tool.name.isBlank()).toList();
    }

    private static CatalogTool normalizeTool(CatalogTool tool) {
        tool.patterns = tool.patterns != null ? tool.patterns : List.of();
        tool.executables = tool.executables != null ? tool.executables : List.of();
        return tool;
    }

    private static void assignScalar(CatalogTool tool, String key, String value) {
        switch (key) {
            case "name" -> tool.name = value;
            case "category" -> tool.category = value;
            case "os" -> tool.os = value;
            default -> {
            }
        }
    }

    private static void assignArray(CatalogTool tool, String key, List<String> values) {
        List<String> items = List.copyOf(values);
        switch (key) {
            case "patterns" -> tool.patterns = items;
            case "executables" -> tool.executables = items;
            default -> {
            }
        }
    }

    private static String stripComments(String line) {
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (c == '#' && !inQuotes) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private static List<String> parseArrayItems(String value) {
        String trimmed = value.trim();
        if (trimmed.endsWith("]")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        if (trimmed.isEmpty()) {
            return List.of();
        }

        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escaping = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (!inQuotes) {
                if (c == '"') {
                    inQuotes = true;
                    current.setLength(0);
                }
                continue;
            }
            if (escaping) {
                current.append(unescape(c));
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (c == '"') {
                items.add(current.toString());
                inQuotes = false;
                continue;
            }
            current.append(c);
        }
        return items;
    }

    private static String parseString(String value) {
        String trimmed = value.trim();
        if (trimmed.endsWith(",")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        StringBuilder parsed = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (escaping) {
                parsed.append(unescape(c));
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else {
                parsed.append(c);
            }
        }
        if (escaping) {
            parsed.append('\\');
        }
        return parsed.toString();
    }

    private static char unescape(char c) {
        return switch (c) {
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case '"' -> '"';
            case '\\' -> '\\';
            default -> c;
        };
    }

    private static String expandPattern(String pattern) {
        // Expand %VAR% and $VAR
        String expanded = pattern;
        // Windows %VAR%
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("%([^%]+)%").matcher(expanded);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String val = System.getenv(m.group(1));
            m.appendReplacement(sb, val != null ? java.util.regex.Matcher.quoteReplacement(val) : m.group(0));
        }
        m.appendTail(sb);
        expanded = sb.toString();
        // Unix $VAR and ${VAR}
        expanded = expanded.replace("$HOME", System.getProperty("user.home", ""));
        expanded = java.util.regex.Pattern.compile("\\$\\{?([A-Za-z_][A-Za-z0-9_]*)\\}?")
            .matcher(expanded)
            .replaceAll(r -> {
                String val = System.getenv(r.group(1));
                return val != null ? java.util.regex.Matcher.quoteReplacement(val) : r.group(0);
            });
        return expanded;
    }

    private static List<Path> glob(String pattern) {
        // Find the non-glob prefix to use as root
        int starIdx = pattern.indexOf('*');
        if (starIdx < 0) {
            Path p = Path.of(pattern);
            return Files.exists(p) ? List.of(p) : List.of();
        }
        int lastSep = Math.max(pattern.lastIndexOf('/'), pattern.lastIndexOf('\\'));
        String root = lastSep > 0 ? pattern.substring(0, lastSep) : ".";
        // String glob  = "glob:" + pattern.replace("\\", "/");

        // Find non-glob root
        int nonGlobEnd = -1;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*' || c == '?' || c == '{' || c == '[') { nonGlobEnd = i; break; }
        }
        if (nonGlobEnd > 0) {
            int sep = Math.max(pattern.lastIndexOf('/', nonGlobEnd), pattern.lastIndexOf('\\', nonGlobEnd));
            root = sep > 0 ? pattern.substring(0, sep) : ".";
        }

        Path rootPath = Path.of(root);
        if (!Files.exists(rootPath)) return List.of();

        try {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher(
                "glob:" + pattern.replace("\\", "/"));
            List<Path> results = new ArrayList<>();
            try (var stream = Files.list(rootPath)) {
                stream.filter(p -> matcher.matches(p) || matcher.matches(
                    Path.of(p.toString().replace("\\", "/"))))
                    .filter(Files::isDirectory)
                    .forEach(results::add);
            }
            return results;
        } catch (Exception e) {
            return List.of();
        }
    }

    private static List<String> listExecutables(File dir, String osName) {
        File[] files = dir.listFiles();
        if (files == null) return List.of();
        List<String> result = new ArrayList<>();
        for (File f : files) {
            if (result.size() >= 8) break;
            if (f.isDirectory()) continue;
            if ("windows".equals(osName)) {
                if (f.getName().toLowerCase().endsWith(".exe"))
                    result.add(f.getName().replaceAll("(?i)\\.exe$", ""));
            } else {
                if (f.canExecute()) result.add(f.getName());
            }
        }
        return result;
    }
}
