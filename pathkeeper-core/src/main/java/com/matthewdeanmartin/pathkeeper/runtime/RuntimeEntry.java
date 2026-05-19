package com.matthewdeanmartin.pathkeeper.runtime;

import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;

public record RuntimeEntry(String value, boolean persisted, Scope scope) {
    /** scope is null when the entry is runtime-only. */
    public boolean isRuntimeOnly() { return !persisted; }
}
