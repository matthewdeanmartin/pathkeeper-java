package com.matthewdeanmartin.pathkeeper.shadow;

import java.util.List;

public record ShadowGroup(String name, List<ShadowEntry> entries) {
    public ShadowEntry winner()         { return entries.get(0); }
    public List<ShadowEntry> shadowed() { return entries.size() <= 1 ? List.of() : entries.subList(1, entries.size()); }
}
