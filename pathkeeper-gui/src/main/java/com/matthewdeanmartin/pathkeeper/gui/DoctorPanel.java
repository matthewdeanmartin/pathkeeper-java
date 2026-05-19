package com.matthewdeanmartin.pathkeeper.gui;

import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.diagnostics.DiagnosticReport;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.DoctorCheck;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;

import javax.swing.*;
import java.awt.*;
import java.util.List;

class DoctorPanel extends JPanel {

    private final AppState state;
    private final JLabel statusBar;
    private final DefaultListModel<DoctorCheck> listModel = new DefaultListModel<>();
    private final JList<DoctorCheck> checkList = new JList<>(listModel);
    private final JTextArea detail = new JTextArea(4, 40);

    DoctorPanel(AppState state, JLabel statusBar) {
        this.state = state;
        this.statusBar = statusBar;
        detail.setEditable(false);
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        checkList.setCellRenderer((list, check, idx, sel, foc) -> {
            String icon = switch (check.status()) {
                case DoctorCheck.PASS -> "✓";
                case DoctorCheck.FAIL -> "✗";
                default               -> "⚠";
            };
            JLabel lbl = new JLabel(icon + "  " + check.name() + " — " + check.detail());
            lbl.setOpaque(true);
            if (sel) {
                lbl.setBackground(list.getSelectionBackground());
                lbl.setForeground(list.getSelectionForeground());
            } else {
                lbl.setBackground(list.getBackground());
                Color fg = switch (check.status()) {
                    case DoctorCheck.FAIL -> new Color(180, 30, 30);
                    case DoctorCheck.WARN -> new Color(160, 100, 0);
                    default               -> list.getForeground();
                };
                lbl.setForeground(fg);
            }
            lbl.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            return lbl;
        });

        checkList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                DoctorCheck sel = checkList.getSelectedValue();
                if (sel != null) {
                    detail.setText(sel.remediation().isBlank() ? "(no remediation needed)" : sel.remediation());
                }
            }
        });

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(ev -> refresh());

        JPanel top = new JPanel(new BorderLayout());
        top.add(new JLabel("<html><b>Doctor checks</b></html>"), BorderLayout.WEST);
        top.add(refresh, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(checkList), BorderLayout.CENTER);
        add(new JScrollPane(detail), BorderLayout.SOUTH);
    }

    void refresh() {
        statusBar.setText("Running doctor checks…");
        SwingWorker<List<DoctorCheck>, Void> worker = new SwingWorker<>() {
            @Override protected List<DoctorCheck> doInBackground() throws Exception {
                Snapshot snap = state.snapshot();
                String raw = Diagnostics.rawForScope(Scope.ALL,
                    snap.systemPathRaw(), snap.userPathRaw(), state.osName);
                DiagnosticReport report = Diagnostics.analyzeSnapshot(
                    snap.systemPath(), snap.userPath(), state.osName, Scope.ALL, raw);
                return Diagnostics.doctorChecks(report);
            }
            @Override protected void done() {
                try {
                    List<DoctorCheck> checks = get();
                    listModel.clear();
                    checks.forEach(listModel::addElement);
                    long fails = checks.stream().filter(c -> DoctorCheck.FAIL.equals(c.status())).count();
                    long warns = checks.stream().filter(c -> DoctorCheck.WARN.equals(c.status())).count();
                    statusBar.setText("Doctor: " + fails + " fail(s), " + warns + " warning(s).");
                } catch (Exception ex) {
                    statusBar.setText("Error: " + ex.getCause().getMessage());
                }
            }
        };
        worker.execute();
    }
}
