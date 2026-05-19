package com.matthewdeanmartin.pathkeeper.diagnostics;

import java.util.List;

public record DiagnosticReport(
    List<DiagnosticEntry> entries,
    DiagnosticSummary summary,
    String osName,
    int pathLength
) {}
