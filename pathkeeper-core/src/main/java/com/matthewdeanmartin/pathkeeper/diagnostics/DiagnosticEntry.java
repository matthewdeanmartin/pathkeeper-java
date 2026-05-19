package com.matthewdeanmartin.pathkeeper.diagnostics;

import java.util.List;

public record DiagnosticEntry(
    int index,
    String value,
    Scope scope,
    boolean exists,
    boolean isDir,
    boolean isDuplicate,
    int duplicateOf,       // 0 = not a duplicate
    boolean isEmpty,
    boolean hasUnexpandedVars,
    String expandedValue,
    List<String> executables,
    boolean likelyMissingSeparator
) {}
