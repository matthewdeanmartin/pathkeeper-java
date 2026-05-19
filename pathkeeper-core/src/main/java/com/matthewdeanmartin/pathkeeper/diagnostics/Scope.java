package com.matthewdeanmartin.pathkeeper.diagnostics;

public enum Scope {
    SYSTEM, USER, ALL;

    public static Scope parse(String s) {
        return switch (s == null ? "all" : s.toLowerCase()) {
            case "system" -> SYSTEM;
            case "user"   -> USER;
            default       -> ALL;
        };
    }
}
