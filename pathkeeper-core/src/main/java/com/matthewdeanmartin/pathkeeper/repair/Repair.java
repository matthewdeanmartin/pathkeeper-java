package com.matthewdeanmartin.pathkeeper.repair;

import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;

import java.util.List;

public record Repair(
    String value,
    int scopeIndex,
    Scope scope,
    int displayIndex,
    List<RepairCandidate> candidates
) {}
