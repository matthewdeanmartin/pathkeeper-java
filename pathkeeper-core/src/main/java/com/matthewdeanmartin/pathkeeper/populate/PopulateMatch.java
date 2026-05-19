package com.matthewdeanmartin.pathkeeper.populate;

import java.util.List;

public record PopulateMatch(String name, String category, String path, List<String> foundExecutables) {}
