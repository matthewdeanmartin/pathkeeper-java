package com.matthewdeanmartin.pathkeeper.gui;

import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.diagnostics.DiagnosticEntry;
import com.matthewdeanmartin.pathkeeper.diagnostics.DiagnosticReport;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

class InspectPanel extends JPanel {

    private final AppState state;
    private final JLabel statusBar;

    private final JComboBox<String> scopeBox   = new JComboBox<>(new String[]{"all", "system", "user"});
    private final JCheckBox onlyInvalidBox      = new JCheckBox("Only invalid");
    private final JCheckBox onlyDupesBox        = new JCheckBox("Only duplicates");
    private final DefaultTableModel tableModel  = new DefaultTableModel(
        new String[]{"#", "Scope", "Exists", "Dir", "Dupe", "Path"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    InspectPanel(AppState state, JLabel statusBar) {
        this.state = state;
        this.statusBar = statusBar;
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(new JLabel("Scope:"));
        toolbar.add(scopeBox);
        toolbar.add(onlyInvalidBox);
        toolbar.add(onlyDupesBox);
        toolbar.add(refresh);

        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    void refresh() {
        statusBar.setText("Inspecting PATH…");
        String scopeStr = (String) scopeBox.getSelectedItem();
        boolean onlyInvalid = onlyInvalidBox.isSelected();
        boolean onlyDupes   = onlyDupesBox.isSelected();

        SwingWorker<List<DiagnosticEntry>, Void> worker = new SwingWorker<>() {
            @Override protected List<DiagnosticEntry> doInBackground() throws Exception {
                Snapshot snap = state.snapshot();
                Scope s = Scope.parse(scopeStr);
                String raw = Diagnostics.rawForScope(s, snap.systemPathRaw(), snap.userPathRaw(), state.osName);
                DiagnosticReport report = Diagnostics.analyzeSnapshot(
                    snap.systemPath(), snap.userPath(), state.osName, s, raw);
                return report.entries().stream()
                    .filter(e -> !onlyInvalid || (!e.exists() || !e.isDir()) && !e.isEmpty())
                    .filter(e -> !onlyDupes   || e.isDuplicate())
                    .toList();
            }
            @Override protected void done() {
                try {
                    List<DiagnosticEntry> entries = get();
                    tableModel.setRowCount(0);
                    for (DiagnosticEntry e : entries) {
                        tableModel.addRow(new Object[]{
                            e.index(),
                            e.scope().name().toLowerCase(),
                            e.exists() ? "yes" : "no",
                            e.isDir() ? "yes" : (e.isEmpty() ? "-" : "no"),
                            e.isDuplicate() ? "dup" : "",
                            e.value().isEmpty() ? "<empty>" : e.value()
                        });
                    }
                    statusBar.setText("Inspect: " + entries.size() + " entries shown.");
                } catch (Exception ex) {
                    statusBar.setText("Error: " + ex.getCause().getMessage());
                }
            }
        };
        worker.execute();
    }
}
