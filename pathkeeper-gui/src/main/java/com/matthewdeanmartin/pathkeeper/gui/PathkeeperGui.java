package com.matthewdeanmartin.pathkeeper.gui;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;

public class PathkeeperGui {

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            JFrame frame = new JFrame("Pathkeeper");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 600);
            frame.add(new JLabel("Pathkeeper GUI — coming soon", SwingConstants.CENTER));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
