package com.matthewdeanmartin.pathkeeper.writer;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import java.io.IOException;
import java.util.List;

public class WindowsPathWriter implements PathWriter {

    private static final String SYSTEM_ENV_KEY =
        "SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment";
    private static final String USER_ENV_KEY = "Environment";

    private final String varName;

    public WindowsPathWriter(String varName) {
        this.varName = (varName == null || varName.isBlank()) ? "Path" : varName;
    }

    @Override
    public void writeSystemPath(List<String> entries) throws IOException {
        String value = String.join(";", entries);
        if (!varName.equalsIgnoreCase("Path")) {
            return;
        }
        writeRegistry(WinReg.HKEY_LOCAL_MACHINE, SYSTEM_ENV_KEY, varName, value);
    }

    @Override
    public void writeUserPath(List<String> entries) throws IOException {
        String value = String.join(";", entries);
        if (!varName.equalsIgnoreCase("Path")) {
            return;
        }
        writeRegistry(WinReg.HKEY_CURRENT_USER, USER_ENV_KEY, varName, value);
    }

    private static void writeRegistry(WinReg.HKEY root, String subKey, String name, String value) throws IOException {
        try {
            Advapi32Util.registrySetExpandableStringValue(root, subKey, name, value);
        } catch (Exception e) {
            throw new IOException("Registry write failed: " + e.getMessage(), e);
        }
    }
}
