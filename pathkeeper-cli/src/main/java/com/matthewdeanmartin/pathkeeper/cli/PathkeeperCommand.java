package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.config.AppDirs;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
    name = "pathkeeper",
    version = "pathkeeper 0.1.0",
    description = "Manage and protect your PATH environment variable.",
    mixinStandardHelpOptions = true,
    subcommands = {
        BackupCommand.class,
        BackupsCommand.class,
        InspectCommand.class,
        DoctorCommand.class,
        RestoreCommand.class,
        DedupeCommand.class,
        PopulateCommand.class,
        RepairTruncatedCommand.class,
        SplitLongCommand.class,
        EditCommand.class,
        ScheduleCommand.class,
        DiffCommand.class,
        DiffCurrentCommand.class,
        ShadowCommand.class,
        RuntimeEntriesCommand.class,
        ShellStartupCommand.class,
        SelfcheckCommand.class,
        LocateCommand.class,
        GuiCommand.class,
        CommandLine.HelpCommand.class,
    }
)
public class PathkeeperCommand implements Callable<Integer> {

    @Option(names = {"--var"}, description = "Environment variable to manage (default: PATH)", defaultValue = "PATH", scope = CommandLine.ScopeType.INHERIT)
    String varName;

    @Option(names = {"--log-level"}, description = "Logging level (debug, info, warn, error)", defaultValue = "warn", scope = CommandLine.ScopeType.INHERIT)
    String logLevel;

    @Option(names = {"--no-color"}, description = "Disable ANSI color output", scope = CommandLine.ScopeType.INHERIT)
    boolean noColor;

    @Override
    public Integer call() throws Exception {
        AppDirs.ensureAppState();
        // No subcommand — show help
        new CommandLine(this).usage(System.out);
        return 0;
    }
}
