package com.matthewdeanmartin.pathkeeper.locate;

import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Locator {

    private static final List<String> WIN_EXTS = List.of(".exe", ".cmd", ".bat", ".com", ".ps1");

    private Locator() {}

    /**
     * Search for an executable by name on the current process PATH.
     * Returns all matches when findAll=true, else stops at the first.
     */
    public static List<String> locate(String name, boolean findAll, String osName) {
        List<String> found = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        String sep = Diagnostics.pathSeparator(osName);
        String livePath = System.getenv("PATH");
        if (livePath == null) return List.of();

        for (String dir : livePath.split(java.util.regex.Pattern.quote(sep), -1)) {
            if (dir.isBlank()) continue;
            if ("windows".equals(osName)) {
                for (String ext : WIN_EXTS) {
                    File candidate = new File(dir, name + ext);
                    if (candidate.isFile() && candidate.canRead()) {
                        String path = resolveSymlinks(candidate.getAbsolutePath());
                        if (seen.add(path)) found.add(path);
                        if (!findAll) return found;
                    }
                }
                // Also try exact name (no extension added)
                File bare = new File(dir, name);
                if (bare.isFile()) {
                    String path = resolveSymlinks(bare.getAbsolutePath());
                    if (seen.add(path)) found.add(path);
                    if (!findAll) return found;
                }
            } else {
                File candidate = new File(dir, name);
                if (candidate.isFile() && candidate.canExecute()) {
                    String path = resolveSymlinks(candidate.getAbsolutePath());
                    if (seen.add(path)) found.add(path);
                    if (!findAll) return found;
                }
            }
        }
        return found;
    }

    private static String resolveSymlinks(String path) {
        try {
            return Path.of(path).toRealPath().toString();
        } catch (Exception e) {
            return path;
        }
    }
}
