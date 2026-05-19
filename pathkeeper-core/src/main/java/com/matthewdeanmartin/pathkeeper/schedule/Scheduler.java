package com.matthewdeanmartin.pathkeeper.schedule;

import java.io.IOException;

public interface Scheduler {
    ScheduleStatus status() throws IOException;
    String install(ScheduleInterval interval, boolean dryRun) throws IOException;
    String remove(boolean dryRun) throws IOException;
}
