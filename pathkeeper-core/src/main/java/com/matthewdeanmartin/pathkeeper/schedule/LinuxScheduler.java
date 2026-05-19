package com.matthewdeanmartin.pathkeeper.schedule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LinuxScheduler implements Scheduler {

    @Override
    public ScheduleStatus status() throws IOException {
        Path timer = timerPath();
        boolean exists = Files.exists(timer);
        return new ScheduleStatus(exists, timer.toString(), timer.toString());
    }

    @Override
    public String install(ScheduleInterval interval, boolean dryRun) throws IOException {
        Path service = servicePath();
        Path timer   = timerPath();
        String serviceContent = renderService();
        String timerContent   = renderTimer(interval);

        if (dryRun) return String.format("[dry-run] Would write:\n%s:\n%s\n%s:\n%s",
            service, serviceContent, timer, timerContent);

        Files.createDirectories(service.getParent());
        Files.writeString(service, serviceContent);
        Files.writeString(timer, timerContent);
        run("systemctl", "--user", "daemon-reload");
        run("systemctl", "--user", "enable", "--now", "pathkeeper.timer");
        return "Installed systemd user timer at " + timer;
    }

    @Override
    public String remove(boolean dryRun) throws IOException {
        if (dryRun) return "[dry-run] Would disable and remove pathkeeper.timer";
        run("systemctl", "--user", "disable", "--now", "pathkeeper.timer");
        Files.deleteIfExists(timerPath());
        Files.deleteIfExists(servicePath());
        run("systemctl", "--user", "daemon-reload");
        return "Removed systemd user timer.";
    }

    private static Path systemdDir() {
        return Path.of(System.getProperty("user.home"), ".config", "systemd", "user");
    }
    private static Path servicePath() { return systemdDir().resolve("pathkeeper.service"); }
    private static Path timerPath()   { return systemdDir().resolve("pathkeeper.timer"); }

    private static String renderService() throws IOException {
        String exe = ProcessHandle.current().info().command().orElse("java");
        return "[Unit]\nDescription=Run pathkeeper backup\n\n[Service]\nType=oneshot\nExecStart="
            + exe + " backup --tag auto\n\n";
    }

    private static String renderTimer(ScheduleInterval interval) {
        StringBuilder sb = new StringBuilder("[Unit]\nDescription=Schedule pathkeeper backups\n\n[Timer]\n");
        switch (interval.kind()) {
            case STARTUP -> sb.append("OnBootSec=1min\nPersistent=true\n");
            case MINUTE, HOUR -> sb.append("OnBootSec=1min\nOnUnitActiveSec=")
                .append(interval.seconds()).append("s\n");
            case DAILY -> sb.append("OnCalendar=*-*-* 03:00:00\nPersistent=true\n");
        }
        sb.append("\n[Install]\nWantedBy=timers.target\n");
        return sb.toString();
    }

    private static void run(String... args) throws IOException {
        try {
            new ProcessBuilder(args).redirectErrorStream(true).start().waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted", e);
        }
    }
}
