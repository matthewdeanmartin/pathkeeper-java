package com.matthewdeanmartin.pathkeeper.schedule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DarwinScheduler implements Scheduler {

    private static final String LABEL = "com.pathkeeper.backup";

    @Override
    public ScheduleStatus status() throws IOException {
        Path plist = plistPath();
        boolean exists = Files.exists(plist);
        return new ScheduleStatus(exists, plist.toString(), plist.toString());
    }

    @Override
    public String install(ScheduleInterval interval, boolean dryRun) throws IOException {
        Path plist = plistPath();
        String content = renderPlist(interval);
        if (dryRun) return "[dry-run] Would write: " + plist + "\n" + content;
        Files.createDirectories(plist.getParent());
        Files.writeString(plist, content);
        run("launchctl", "load", plist.toString());
        return "Installed launchd agent at " + plist;
    }

    @Override
    public String remove(boolean dryRun) throws IOException {
        Path plist = plistPath();
        if (dryRun) return "[dry-run] Would unload and remove: " + plist;
        if (!Files.exists(plist)) return "No launchd agent to remove.";
        run("launchctl", "unload", plist.toString());
        Files.deleteIfExists(plist);
        return "Removed launchd agent: " + plist;
    }

    private static Path plistPath() {
        return Path.of(System.getProperty("user.home"),
            "Library", "LaunchAgents", LABEL + ".plist");
    }

    private static String renderPlist(ScheduleInterval interval) throws IOException {
        String exe = ProcessHandle.current().info().command().orElse("java");
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        sb.append("<plist version=\"1.0\">\n<dict>\n");
        sb.append("  <key>Label</key><string>").append(LABEL).append("</string>\n");
        sb.append("  <key>ProgramArguments</key>\n  <array>\n");
        sb.append("    <string>").append(xmlEscape(exe)).append("</string>\n");
        sb.append("    <string>backup</string><string>--tag</string><string>auto</string>\n");
        sb.append("  </array>\n");
        switch (interval.kind()) {
            case STARTUP -> sb.append("  <key>RunAtLoad</key><true/>\n");
            case MINUTE, HOUR -> sb.append("  <key>StartInterval</key><integer>")
                .append(interval.seconds()).append("</integer>\n");
            case DAILY -> sb.append("  <key>StartCalendarInterval</key>\n  <dict>\n")
                .append("    <key>Hour</key><integer>3</integer>\n")
                .append("    <key>Minute</key><integer>0</integer>\n  </dict>\n");
        }
        sb.append("</dict>\n</plist>\n");
        return sb.toString();
    }

    private static void run(String... args) throws IOException {
        try {
            Process p = new ProcessBuilder(args).redirectErrorStream(true).start();
            p.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted", e);
        }
    }

    private static String xmlEscape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
