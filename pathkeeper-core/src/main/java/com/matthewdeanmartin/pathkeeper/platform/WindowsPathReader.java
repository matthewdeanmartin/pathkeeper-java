package com.matthewdeanmartin.pathkeeper.platform;

import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WindowsPathReader implements PathReader {

    private static final String SYSTEM_ENV_KEY =
        "SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment";
    private static final String USER_ENV_KEY = "Environment";

    @Override
    public Snapshot readSnapshot() throws java.io.IOException {
        return readSnapshotVar("PATH");
    }

    @Override
    public Snapshot readSnapshotVar(String varName) throws java.io.IOException {
        if (varName == null || varName.isBlank() || varName.equalsIgnoreCase("PATH")) {
            return readWindowsPath();
        }
        // Non-PATH variable: read from the current process environment so tests
        // can set PATHX=... without needing registry access.
        String raw = System.getenv(varName);
        if (raw == null) raw = "";
        return new Snapshot(
            Collections.emptyList(),
            splitPath(raw),
            "",
            raw,
            Map.of(),
            Map.of()
        );
    }

    private Snapshot readWindowsPath() {
        String systemRaw = readRegValue(WinReg.HKEY_LOCAL_MACHINE, SYSTEM_ENV_KEY, "Path");
        String userRaw   = readRegValue(WinReg.HKEY_CURRENT_USER,  USER_ENV_KEY,   "Path");
        return new Snapshot(
            splitPath(systemRaw),
            splitPath(userRaw),
            systemRaw,
            userRaw,
            Map.of(),
            Map.of()
        );
    }

    private static String readRegValue(WinReg.HKEY root, String subKey, String valueName) {
        try {
            if (!Advapi32Util.registryKeyExists(root, subKey)) return "";
            if (!Advapi32Util.registryValueExists(root, subKey, valueName)) return "";
            Object val = Advapi32Util.registryGetValue(root, subKey, valueName);
            return val != null ? val.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static List<String> splitPath(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        return Arrays.asList(raw.split(";", -1));
    }
}
