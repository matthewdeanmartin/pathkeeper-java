package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.schedule.ScheduleInterval;
import com.matthewdeanmartin.pathkeeper.schedule.ScheduleStatus;
import com.matthewdeanmartin.pathkeeper.schedule.Schedulers;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
    name = "schedule",
    description = "Manage the pathkeeper scheduled backup task.",
    mixinStandardHelpOptions = true,
    subcommands = {
        ScheduleCommand.InstallCommand.class,
        ScheduleCommand.RemoveCommand.class,
        ScheduleCommand.StatusCommand.class,
    }
)
public class ScheduleCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.err.println("Specify a subcommand: install, remove, status");
        return 1;
    }

    // -------------------------------------------------------------------------

    @Command(name = "install", description = "Install the scheduled backup task.", mixinStandardHelpOptions = true)
    static class InstallCommand implements Callable<Integer> {

        @Option(names = {"--interval"}, description = "startup, daily, <N>m, or <N>h (default: startup)", defaultValue = "startup")
        String interval;

        @Option(names = {"--dry-run"}, description = "Show what would be installed without writing")
        boolean dryRun;

        @Override
        public Integer call() throws Exception {
            ScheduleInterval si;
            try {
                si = ScheduleInterval.parse(interval);
            } catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
                return 1;
            }
            String result = Schedulers.create().install(si, dryRun);
            System.out.println(result);
            return 0;
        }
    }

    // -------------------------------------------------------------------------

    @Command(name = "remove", description = "Remove the scheduled backup task.", mixinStandardHelpOptions = true)
    static class RemoveCommand implements Callable<Integer> {

        @Option(names = {"--dry-run"}, description = "Show what would be removed without running")
        boolean dryRun;

        @Override
        public Integer call() throws Exception {
            String result = Schedulers.create().remove(dryRun);
            System.out.println(result);
            return 0;
        }
    }

    // -------------------------------------------------------------------------

    @Command(name = "status", description = "Show status of the scheduled backup task.", mixinStandardHelpOptions = true)
    static class StatusCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            ScheduleStatus status = Schedulers.create().status();
            System.out.println("Enabled: " + status.enabled());
            System.out.println("Source:  " + status.source());
            if (!status.detail().isBlank()) System.out.println("Detail:  " + status.detail());
            return status.enabled() ? 0 : 1;
        }
    }
}
