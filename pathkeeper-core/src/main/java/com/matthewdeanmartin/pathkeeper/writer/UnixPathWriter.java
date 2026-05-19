package com.matthewdeanmartin.pathkeeper.writer;

import java.io.IOException;
import java.util.List;

/**
 * Unix PATH writer: writes to the current process environment only.
 * Full rc-file persistence (shell-startup) is a Phase 4 feature.
 */
public class UnixPathWriter implements PathWriter {

    private final String varName;

    public UnixPathWriter(String varName) {
        this.varName = (varName == null || varName.isBlank()) ? "PATH" : varName;
    }

    @Override
    public void writeSystemPath(List<String> entries) throws IOException {
        // On Unix, "system" PATH is effectively the same variable
        writeUserPath(entries);
    }

    @Override
    public void writeUserPath(List<String> entries) throws IOException {
        String value = String.join(":", entries);
        try {
            setEnv(varName, value);
        } catch (Exception e) {
            throw new IOException("Cannot update process environment for " + varName, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setEnv(String name, String value) throws Exception {
        // Reflective approach — works on OpenJDK and IBM Semeru
        try {
            Class<?> pe = Class.forName("java.lang.ProcessEnvironment");
            var f = pe.getDeclaredField("theEnvironment");
            f.setAccessible(true);
            ((java.util.Map<String, String>) f.get(null)).put(name, value);
        } catch (NoSuchFieldException e) {
            // Fallback for some JVM impls
            var env = System.getenv();
            var field = env.getClass().getDeclaredField("m");
            field.setAccessible(true);
            ((java.util.Map<String, String>) field.get(env)).put(name, value);
        }
    }
}
