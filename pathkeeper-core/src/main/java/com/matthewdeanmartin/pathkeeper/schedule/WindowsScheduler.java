package com.matthewdeanmartin.pathkeeper.schedule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WindowsScheduler implements Scheduler {

    private static final String TASK_NAME = "pathkeeper";

    @Override
    public ScheduleStatus status() throws IOException {
        try {
            String out = run("schtasks", "/Query", "/TN", TASK_NAME);
            return new ScheduleStatus(true, out.isBlank() ? "scheduled task installed" : out.strip(), TASK_NAME);
        } catch (IOException e) {
            return new ScheduleStatus(false, "task not found", TASK_NAME);
        }
    }

    @Override
    public String install(ScheduleInterval interval, boolean dryRun) throws IOException {
        String exe = ProcessHandle.current().info().command().orElse("java");
        String cmdLine = "\"" + exe + "\" backup --tag auto";

        List<String> args = new ArrayList<>(List.of(
            "schtasks", "/Create", "/F", "/TN", TASK_NAME, "/TR", cmdLine));

        switch (interval.kind()) {
            case STARTUP -> args.addAll(List.of("/SC", "ONSTART"));
            case MINUTE, HOUR -> args.addAll(List.of(
                "/SC", "MINUTE", "/MO", String.valueOf(interval.minutes()), "/ST", "00:00"));
            case DAILY -> args.addAll(List.of("/SC", "DAILY", "/ST", "03:00"));
        }

        String preview = String.join(" ", args);
        if (dryRun) return "[dry-run] Would run: " + preview;
        run(args.toArray(String[]::new));
        return "Installed Windows scheduled task: " + TASK_NAME;
    }

    @Override
    public String remove(boolean dryRun) throws IOException {
        String preview = "schtasks /Delete /F /TN " + TASK_NAME;
        if (dryRun) return "[dry-run] Would run: " + preview;
        try {
            run("schtasks", "/Delete", "/F", "/TN", TASK_NAME);
            return "Removed Windows scheduled task: " + TASK_NAME;
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("cannot find")) {
                return "No scheduled task to remove.";
            }
            throw e;
        }
    }

    private static String run(String... args) throws IOException {
        try {
            Process p = new ProcessBuilder(args)
                .redirectErrorStream(true)
                .start();
            String out = new String(p.getInputStream().readAllBytes()).strip();
            int exit = p.waitFor();
            if (exit != 0) throw new IOException(out.isBlank() ? "exit " + exit : out);
            return out;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted", e);
        }
    }
}
