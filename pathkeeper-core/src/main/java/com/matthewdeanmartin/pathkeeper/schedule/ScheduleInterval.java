package com.matthewdeanmartin.pathkeeper.schedule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ScheduleInterval(Kind kind, int value) {

    public enum Kind { STARTUP, MINUTE, HOUR, DAILY }

    private static final Pattern PATTERN = Pattern.compile("^([1-9][0-9]*)([mh])$");

    public static ScheduleInterval parse(String raw) {
        if (raw == null) return startup();
        String s = raw.strip().toLowerCase();
        return switch (s) {
            case "startup", "" -> startup();
            case "daily"       -> new ScheduleInterval(Kind.DAILY, 0);
            default -> {
                Matcher m = PATTERN.matcher(s);
                if (!m.matches()) throw new IllegalArgumentException(
                    "Invalid interval \"" + raw + "\" (expected startup, daily, <N>m, or <N>h)");
                int n = Integer.parseInt(m.group(1));
                yield "m".equals(m.group(2))
                    ? new ScheduleInterval(Kind.MINUTE, n)
                    : new ScheduleInterval(Kind.HOUR, n);
            }
        };
    }

    public static ScheduleInterval startup() { return new ScheduleInterval(Kind.STARTUP, 0); }
    public static ScheduleInterval daily()   { return new ScheduleInterval(Kind.DAILY,   0); }

    public int minutes() {
        return switch (kind) {
            case MINUTE -> value;
            case HOUR   -> value * 60;
            default     -> 0;
        };
    }

    public int seconds() { return minutes() * 60; }

    @Override public String toString() {
        return switch (kind) {
            case STARTUP -> "startup";
            case DAILY   -> "daily";
            case MINUTE  -> value + "m";
            case HOUR    -> value + "h";
        };
    }
}
