package com.matthewdeanmartin.pathkeeper.cli;

import com.matthewdeanmartin.pathkeeper.gui.PathkeeperGui;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
    name = "gui",
    description = "Launch the Swing GUI.",
    mixinStandardHelpOptions = true
)
public class GuiCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        PathkeeperGui.launch();
        return 0;
    }
}
