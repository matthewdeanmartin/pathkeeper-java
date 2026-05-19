package com.matthewdeanmartin.pathkeeper.dedupe;

import java.util.List;

public record DedupeResult(
    List<String> original,
    List<String> cleaned,
    List<String> removedDuplicates,
    List<String> removedInvalid,
    List<String> removedEmpty
) {
    public boolean changed() {
        return !original.equals(cleaned);
    }
}
