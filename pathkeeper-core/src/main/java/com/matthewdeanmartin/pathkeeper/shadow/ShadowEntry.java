package com.matthewdeanmartin.pathkeeper.shadow;

import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;

public record ShadowEntry(String directory, Scope scope, int index) {}
