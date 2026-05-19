package com.matthewdeanmartin.pathkeeper.cli;

import picocli.CommandLine.Command;

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

    @Command(name = "install", description = "Install the scheduled backup task.", mixinStandardHelpOptions = true)
    static class InstallCommand implements Callable<Integer> {
        @Override public Integer call() {
            System.err.println("[schedule install] Not yet implemented.");
            return 1;
        }
    }

    @Command(name = "remove", description = "Remove the scheduled backup task.", mixinStandardHelpOptions = true)
    static class RemoveCommand implements Callable<Integer> {
        @Override public Integer call() {
            System.err.println("[schedule remove] Not yet implemented.");
            return 1;
        }
    }

    @Command(name = "status", description = "Show status of the scheduled backup task.", mixinStandardHelpOptions = true)
    static class StatusCommand implements Callable<Integer> {
        @Override public Integer call() {
            System.err.println("[schedule status] Not yet implemented.");
            return 1;
        }
    }
}
