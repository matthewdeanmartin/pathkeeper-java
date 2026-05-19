package com.matthewdeanmartin.pathkeeper.gui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.matthewdeanmartin.pathkeeper.config.AppDirs;

import javax.swing.*;
import java.awt.*;

public class PathkeeperGui {

    public static void launch() throws Exception {
        AppDirs.ensureAppState();

        // Latch released when the JFrame's windowClosed event fires.
        java.util.concurrent.CountDownLatch closed = new java.util.concurrent.CountDownLatch(1);

        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            buildAndShow(closed);
        });

        // Block the CLI thread here until the user closes the window.
        closed.await();
    }

    private static void buildAndShow(java.util.concurrent.CountDownLatch closed) {
        AppState state = new AppState();
        JFrame frame = new JFrame("Pathkeeper");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { closed.countDown(); }
        });
        frame.setSize(1050, 680);
        frame.setLocationRelativeTo(null);

        JLabel statusBar = new JLabel(" Ready");
        statusBar.setBorder(BorderFactory.createEtchedBorder());

        DashboardPanel dashboard = new DashboardPanel(state, statusBar);
        InspectPanel   inspect   = new InspectPanel(state, statusBar);
        DoctorPanel    doctor    = new DoctorPanel(state, statusBar);
        BackupsPanel   backups   = new BackupsPanel(state, statusBar);
        RestorePanel   restore   = new RestorePanel(state, statusBar);
        DedupePanel    dedupe    = new DedupePanel(state, statusBar);

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.LEFT);
        tabs.addTab("Dashboard", dashboard);
        tabs.addTab("Inspect",   inspect);
        tabs.addTab("Doctor",    doctor);
        tabs.addTab("Backups",   backups);
        tabs.addTab("Restore",   restore);
        tabs.addTab("Dedupe",    dedupe);

        tabs.addChangeListener(e -> {
            Component sel = tabs.getSelectedComponent();
            if      (sel == dashboard) dashboard.refresh();
            else if (sel == inspect)   inspect.refresh();
            else if (sel == doctor)    doctor.refresh();
            else if (sel == backups)   backups.refresh();
            else if (sel == restore)   restore.refresh();
            else if (sel == dedupe)    dedupe.refresh();
        });

        frame.setLayout(new BorderLayout());
        frame.add(tabs, BorderLayout.CENTER);
        frame.add(statusBar, BorderLayout.SOUTH);
        frame.setVisible(true);

        // Trigger initial data load for the first tab
        dashboard.refresh();
    }
}
