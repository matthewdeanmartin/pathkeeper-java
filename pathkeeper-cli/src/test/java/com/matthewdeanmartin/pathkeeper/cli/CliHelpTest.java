package com.matthewdeanmartin.pathkeeper.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

class CliHelpTest {

    private CommandLine cli() {
        return new CommandLine(new PathkeeperCommand());
    }

    @Test
    void versionExitsZero() {
        StringWriter out = new StringWriter();
        CommandLine cmd = cli();
        cmd.setOut(new PrintWriter(out));
        int exit = cmd.execute("--version");
        assertThat(exit).isEqualTo(0);
        assertThat(out.toString().trim()).isNotEmpty();
    }

    @Test
    void rootHelpExitsZero() {
        int exit = cli().execute("--help");
        assertThat(exit).isEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "inspect", "doctor", "backup", "backups",
        "restore", "dedupe", "populate", "repair-truncated",
        "split-long", "edit", "schedule", "diff",
        "diff-current", "shadow", "runtime-entries",
        "shell-startup", "selfcheck", "locate", "gui"
    })
    void subcommandHelpExitsZero(String sub) {
        int exit = cli().execute(sub, "--help");
        assertThat(exit).isEqualTo(0);
    }

    @Test
    void backupsListHelp() {
        int exit = cli().execute("backups", "list", "--help");
        assertThat(exit).isEqualTo(0);
    }

    @Test
    void backupsShowHelp() {
        int exit = cli().execute("backups", "show", "--help");
        assertThat(exit).isEqualTo(0);
    }

    @Test
    void scheduleInstallHelp() {
        int exit = cli().execute("schedule", "install", "--help");
        assertThat(exit).isEqualTo(0);
    }

    @Test
    void scheduleRemoveHelp() {
        int exit = cli().execute("schedule", "remove", "--help");
        assertThat(exit).isEqualTo(0);
    }

    @Test
    void scheduleStatusHelp() {
        int exit = cli().execute("schedule", "status", "--help");
        assertThat(exit).isEqualTo(0);
    }
}
