package com.matthewdeanmartin.pathkeeper.gui;

import com.matthewdeanmartin.pathkeeper.backup.BackupRecord;
import com.matthewdeanmartin.pathkeeper.backup.BackupStore;
import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;
import com.matthewdeanmartin.pathkeeper.diff.PathDiff;
import com.matthewdeanmartin.pathkeeper.writer.PathWriters;

import javax.swing.*;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

class RestorePanel extends JPanel {

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final AppState state;
    private final JLabel statusBar;
    private final DefaultListModel<BackupRecord> listModel = new DefaultListModel<>();
    private final JList<BackupRecord> backupList = new JList<>(listModel);
    private final JTextArea diffArea = new JTextArea(8, 40);

    RestorePanel(AppState state, JLabel statusBar) {
        this.state = state;
        this.statusBar = statusBar;
        diffArea.setEditable(false);
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        backupList.setCellRenderer((list, rec, idx, sel, foc) -> {
            JLabel lbl = new JLabel((idx + 1) + ".  " + FMT.format(rec.timestamp) + "  [" + rec.tag + "]");
            lbl.setOpaque(true);
            lbl.setBackground(sel ? list.getSelectionBackground() : list.getBackground());
            lbl.setForeground(sel ? list.getSelectionForeground() : list.getForeground());
            lbl.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            return lbl;
        });
        backupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) previewDiff();
        });

        JButton refresh  = new JButton("Refresh");
        JButton restore  = new JButton("Restore selected");
        refresh.addActionListener(e -> refresh());
        restore.addActionListener(e -> doRestore());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(refresh);
        toolbar.add(restore);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            new JScrollPane(backupList), new JScrollPane(diffArea));
        split.setResizeWeight(0.4);

        add(toolbar, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    private void previewDiff() {
        BackupRecord selected = backupList.getSelectedValue();
        if (selected == null) return;
        statusBar.setText("Computing diff…");
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override protected String doInBackground() throws Exception {
                Snapshot current = state.snapshot();
                List<String> backupEntries  = Diagnostics.entriesForScope(Scope.ALL, selected.systemPath, selected.userPath);
                List<String> currentEntries = Diagnostics.entriesForScope(Scope.ALL, current.systemPath(), current.userPath());
                return PathDiff.compute(currentEntries, backupEntries, state.osName).render();
            }
            @Override protected void done() {
                try { diffArea.setText(get()); statusBar.setText("Diff ready."); }
                catch (Exception ex) { statusBar.setText("Error: " + ex.getCause().getMessage()); }
            }
        };
        worker.execute();
    }

    private void doRestore() {
        BackupRecord selected = backupList.getSelectedValue();
        if (selected == null) { statusBar.setText("No backup selected."); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Restore PATH from backup:\n" + FMT.format(selected.timestamp) + "?",
            "Confirm restore", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        statusBar.setText("Restoring…");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                Snapshot current = state.snapshot();
                BackupStore.create(current, state.backupsDir(), "auto", "pre-restore", false);
                Snapshot updated = new Snapshot(selected.systemPath, selected.userPath,
                    selected.systemPathRaw, selected.userPathRaw,
                    current.systemEnvVars(), current.userEnvVars());
                PathWriters.writeChanged(PathWriters.create(state.varName), current, updated, Scope.ALL);
                return null;
            }
            @Override protected void done() {
                try { get(); statusBar.setText("PATH restored."); }
                catch (Exception ex) { statusBar.setText("Error: " + ex.getCause().getMessage()); }
            }
        };
        worker.execute();
    }

    void refresh() {
        statusBar.setText("Loading backups…");
        SwingWorker<List<BackupRecord>, Void> worker = new SwingWorker<>() {
            @Override protected List<BackupRecord> doInBackground() throws Exception { return state.backups(); }
            @Override protected void done() {
                try {
                    List<BackupRecord> records = get();
                    listModel.clear(); records.forEach(listModel::addElement);
                    statusBar.setText("Backups loaded: " + records.size());
                } catch (Exception ex) { statusBar.setText("Error: " + ex.getCause().getMessage()); }
            }
        };
        worker.execute();
    }
}
