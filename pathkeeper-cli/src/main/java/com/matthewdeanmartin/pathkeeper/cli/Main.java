package com.matthewdeanmartin.pathkeeper.cli;

import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        int exit = new CommandLine(new PathkeeperCommand()).execute(args);
        System.exit(exit);
    }
}
