package com.matthewdeanmartin.pathkeeper.platform;

public final class PathReaders {

    private PathReaders() {}

    public static PathReader create() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return new WindowsPathReader();
        }
        return new UnixPathReader();
    }
}
