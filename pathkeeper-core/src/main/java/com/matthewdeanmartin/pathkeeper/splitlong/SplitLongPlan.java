package com.matthewdeanmartin.pathkeeper.splitlong;

import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;

import java.util.List;
import java.util.Map;

public record SplitLongPlan(
    Scope scope,
    List<String> originalEntries,
    List<String> updatedEntries,
    String originalRaw,
    String updatedRaw,
    Map<String, String> helperVars,
    String varPrefix,
    int maxLength,
    int chunkLength,
    boolean changed
) {}
