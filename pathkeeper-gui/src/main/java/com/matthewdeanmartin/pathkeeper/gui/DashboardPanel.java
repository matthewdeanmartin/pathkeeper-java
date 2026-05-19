package com.matthewdeanmartin.pathkeeper.gui;

import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.diagnostics.DiagnosticReport;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;

import javax.swing.*;
import java.awt.*;

class DashboardPanel extends JPanel {

    private final AppState state;
    private final JLabel statusBar;

    private final JLabel osValue      = new JLabel("-");
    private final JLabel varValue     = new JLabel("-");
    private final JLabel totalValue   = new JLabel("-");
    private final JLabel invalidValue = new JLabel("-");
    private final JLabel dupValue     = new JLabel("-");
    private final JLabel lengthValue  = new JLabel("-");
    private final JLabel backupValue  = new JLabel("-");
    private final JTextArea warnings  = new JTextArea(3, 40);

    DashboardPanel(AppState state, JLabel statusBar) {
        this.state = state;
        this.statusBar = statusBar;
        warnings.setEditable(false);
        warnings.setLineWrap(true);
        warnings.setWrapStyleWord(true);
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST; lc.insets = new Insets(3, 3, 3, 12); lc.gridx = 0;
        GridBagConstraints vc = new GridBagConstraints();
        vc.anchor = GridBagConstraints.WEST; vc.insets = new Insets(3, 0, 3, 0); vc.gridx = 1; vc.fill = GridBagConstraints.HORIZONTAL; vc.weightx = 1;

        String[][] rows = {{"Operating system", null}, {"Managed variable", null},
            {"Total entries", null}, {"Invalid entries", null},
            {"Duplicate entries", null}, {"PATH length", null}, {"Backups on disk", null}};
        JLabel[] values = {osValue, varValue, totalValue, invalidValue, dupValue, lengthValue, backupValue};

        for (int i = 0; i < rows.length; i++) {
            lc.gridy = vc.gridy = i;
            form.add(new JLabel(rows[i][0] + ":"), lc);
            form.add(values[i], vc);
        }

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());

        JPanel top = new JPanel(new BorderLayout());
        top.add(new JLabel("<html><b>Dashboard</b></html>"), BorderLayout.WEST);
        top.add(refresh, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(new JLabel("<html><b>Warnings</b></html>"), BorderLayout.NORTH);
        bottom.add(new JScrollPane(warnings), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    void refresh() {
        statusBar.setText("Loading dashboard…");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private String os, var, total, invalid, dup, length, backups, warn;
            @Override protected Void doInBackground() throws Exception {
                Snapshot snap = state.snapshot();
                String raw = Diagnostics.rawForScope(Scope.ALL,
                    snap.systemPathRaw(), snap.userPathRaw(), state.osName);
                DiagnosticReport report = Diagnostics.analyzeSnapshot(
                    snap.systemPath(), snap.userPath(), state.osName, Scope.ALL, raw);
                os      = state.osName;
                var     = state.varName;
                total   = report.summary().total() + " (system " + snap.systemPath().size()
                          + ", user " + snap.userPath().size() + ")";
                invalid = String.valueOf(report.summary().invalid());
                dup     = String.valueOf(report.summary().duplicates());
                length  = report.pathLength() + " chars";
                backups = String.valueOf(state.backups().size());
                warn    = report.summary().warnings().isEmpty()
                          ? "(none)" : String.join("\n", report.summary().warnings());
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    osValue.setText(os); varValue.setText(var); totalValue.setText(total);
                    invalidValue.setText(invalid); dupValue.setText(dup);
                    lengthValue.setText(length); backupValue.setText(backups);
                    warnings.setText(warn);
                    statusBar.setText("Dashboard refreshed.");
                } catch (Exception e) {
                    statusBar.setText("Error: " + e.getCause().getMessage());
                }
            }
        };
        worker.execute();
    }
}
