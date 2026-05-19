package com.matthewdeanmartin.pathkeeper.schedule;

public final class Schedulers {
    private Schedulers() {}

    public static Scheduler create() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win"))  return new WindowsScheduler();
        if (os.contains("mac"))  return new DarwinScheduler();
        return new LinuxScheduler();
    }
}
