package com.matthewdeanmartin.pathkeeper.shellstartup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ShellStartup {

    private static final String MARKER = "# pathkeeper auto-backup";

    private ShellStartup() {}

    /** Detect whether the hook is already installed in any known rc file. */
    public static Optional<Path> detect(String shell, Path rcFile) {
        for (Path rc : candidateFiles(shell, rcFile)) {
            try {
                if (Files.exists(rc) && Files.readString(rc).contains(MARKER)) {
                    return Optional.of(rc);
                }
            } catch (IOException ignored) {}
        }
        return Optional.empty();
    }

    /** Add the hook to the most appropriate rc file. Returns the file modified. */
    public static Path install(boolean dryRun, String shell, Path rcFile) throws IOException {
        Optional<Path> existing = detect(shell, rcFile);
        if (existing.isPresent()) {
            System.out.println("Hook already installed in " + existing.get());
            return existing.get();
        }

        Path target = chooseTarget(shell, rcFile);
        String snippet = "\n" + MARKER + "\n" + hookLine(shell) + "\n";

        if (dryRun) {
            System.out.println("[dry-run] Would append to " + target + ":\n" + snippet);
            return target;
        }
        Files.createDirectories(target.getParent());
        Files.writeString(target, snippet, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        return target;
    }

    /** Remove the hook from whichever rc file contains it. */
    public static Optional<Path> uninstall(boolean dryRun, String shell, Path rcFile) throws IOException {
        Optional<Path> found = detect(shell, rcFile);
        if (found.isEmpty() && rcFile != null) {
            return Optional.empty();
        }
        if (found.isEmpty()) return Optional.empty();
        Path rc = found.get();

        if (dryRun) {
            System.out.println("[dry-run] Would remove hook from " + rc);
            return Optional.of(rc);
        }

        List<String> lines = Files.readAllLines(rc);
        String hookLine = hookLine(shellForFile(shell, rc));
        List<String> filtered = lines.stream()
            .filter(l -> !l.strip().equals(MARKER) && !l.strip().equals(hookLine))
            .toList();
        Files.write(rc, filtered);
        return Optional.of(rc);
    }

    private static List<Path> candidateFiles(String shell, Path rcFile) {
        if (rcFile != null) {
            return List.of(rcFile);
        }
        String home = System.getProperty("user.home");
        if ("powershell".equalsIgnoreCase(shell)) {
            return List.of(Path.of(home, "Documents", "WindowsPowerShell", "profile.ps1"));
        }
        if ("zsh".equalsIgnoreCase(shell)) {
            return List.of(Path.of(home, ".zshrc"));
        }
        List<Path> defaults = new ArrayList<>();
        defaults.add(Path.of(home, ".bashrc"));
        defaults.add(Path.of(home, ".bash_profile"));
        defaults.add(Path.of(home, ".zshrc"));
        defaults.add(Path.of(home, ".profile"));
        return defaults;
    }

    private static Path chooseTarget(String shell, Path rcFile) {
        if (rcFile != null) {
            return rcFile;
        }
        String detectedShell = System.getenv("SHELL");
        String home  = System.getProperty("user.home");
        if ("powershell".equalsIgnoreCase(shell)) return Path.of(home, "Documents", "WindowsPowerShell", "profile.ps1");
        if ("zsh".equalsIgnoreCase(shell) || (detectedShell != null && detectedShell.contains("zsh"))) {
            return Path.of(home, ".zshrc");
        }
        if ("bash".equalsIgnoreCase(shell) || (detectedShell != null && detectedShell.contains("bash"))) {
            Path bashrc = Path.of(home, ".bashrc");
            return Files.exists(bashrc) ? bashrc : Path.of(home, ".bash_profile");
        }
        return Path.of(home, ".profile");
    }

    private static String hookLine(String shell) {
        if ("powershell".equalsIgnoreCase(shell)) {
            return "pathkeeper backup --tag auto *> $null";
        }
        return "pathkeeper backup --tag auto 2>/dev/null || true";
    }

    private static String shellForFile(String shell, Path rcFile) {
        if (shell != null && !shell.isBlank()) {
            return shell;
        }
        String name = rcFile.getFileName().toString().toLowerCase();
        if (name.endsWith(".ps1")) {
            return "powershell";
        }
        if (name.contains("zsh")) {
            return "zsh";
        }
        return "bash";
    }
}
