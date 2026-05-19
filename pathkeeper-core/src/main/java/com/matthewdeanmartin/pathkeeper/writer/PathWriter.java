package com.matthewdeanmartin.pathkeeper.writer;

import java.io.IOException;
import java.util.List;

public interface PathWriter {
    void writeSystemPath(List<String> entries) throws IOException;
    void writeUserPath(List<String> entries) throws IOException;
}
