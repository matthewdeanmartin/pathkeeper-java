package com.matthewdeanmartin.pathkeeper.shellstartup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

public final class ShellStartup {

    private static final String MARKER = "# pathkeeper auto-backup";
    private static final String HOOK_LINE = "pathkeeper backup --tag auto --quiet 2>/dev/null || true";

    private ShellStartup() {}

    /** Detect whether the hook is already installed in any known rc file. */
    public static Optional<Path> detect() {
        for (Path rc : candidateFiles()) {
            try {
                if (Files.exists(rc) && Files.readString(rc).contains(MARKER)) {
                    return Optional.of(rc);
                }
            } catch (IOException ignored) {}
        }
        return Optional.empty();
    }

    /** Add the hook to the most appropriate rc file. Returns the file modified. */
    public static Path install(boolean dryRun) throws IOException {
        Optional<Path> existing = detect();
        if (existing.isPresent()) {
            System.out.println("Hook already installed in " + existing.get());
            return existing.get();
        }

        Path target = chooseTarget();
        String snippet = "\n" + MARKER + "\n" + HOOK_LINE + "\n";

        if (dryRun) {
            System.out.println("[dry-run] Would append to " + target + ":\n" + snippet);
            return target;
        }
        Files.createDirectories(target.getParent());
        Files.writeString(target, snippet, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        return target;
    }

    /** Remove the hook from whichever rc file contains it. */
    public static Optional<Path> uninstall(boolean dryRun) throws IOException {
        Optional<Path> found = detect();
        if (found.isEmpty()) return Optional.empty();
        Path rc = found.get();

        if (dryRun) {
            System.out.println("[dry-run] Would remove hook from " + rc);
            return Optional.of(rc);
        }

        List<String> lines = Files.readAllLines(rc);
        List<String> filtered = lines.stream()
            .filter(l -> !l.strip().equals(MARKER) && !l.strip().equals(HOOK_LINE))
            .toList();
        Files.write(rc, filtered);
        return Optional.of(rc);
    }

    private static List<Path> candidateFiles() {
        String home = System.getProperty("user.home");
        return List.of(
            Path.of(home, ".bashrc"),
            Path.of(home, ".bash_profile"),
            Path.of(home, ".zshrc"),
            Path.of(home, ".profile")
        );
    }

    private static Path chooseTarget() {
        String shell = System.getenv("SHELL");
        String home  = System.getProperty("user.home");
        if (shell != null && shell.contains("zsh")) return Path.of(home, ".zshrc");
        if (shell != null && shell.contains("bash")) {
            Path bashrc = Path.of(home, ".bashrc");
            return Files.exists(bashrc) ? bashrc : Path.of(home, ".bash_profile");
        }
        return Path.of(home, ".profile");
    }
}
