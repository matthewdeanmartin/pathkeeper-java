package com.matthewdeanmartin.pathkeeper.diagnostics;

import java.util.List;

public record DiagnosticSummary(
    int total,
    int valid,
    int invalid,
    int duplicates,
    int empty,
    int files,
    int missingSeparators,
    List<String> warnings
) {}
