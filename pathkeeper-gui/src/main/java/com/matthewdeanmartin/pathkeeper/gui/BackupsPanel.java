package com.matthewdeanmartin.pathkeeper.gui;

import com.matthewdeanmartin.pathkeeper.backup.BackupRecord;
import com.matthewdeanmartin.pathkeeper.backup.BackupStore;

import javax.swing.*;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

class BackupsPanel extends JPanel {

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final AppState state;
    private final JLabel statusBar;
    private final DefaultListModel<BackupRecord> listModel = new DefaultListModel<>();
    private final JList<BackupRecord> backupList = new JList<>(listModel);
    private final JTextArea detail = new JTextArea(6, 40);
    private final JTextField noteField = new JTextField(20);

    BackupsPanel(AppState state, JLabel statusBar) {
        this.state = state;
        this.statusBar = statusBar;
        detail.setEditable(false);
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        backupList.setCellRenderer((list, rec, idx, sel, foc) -> {
            String ts = FMT.format(rec.timestamp);
            String note = (rec.note != null && !rec.note.isBlank()) ? "  " + rec.note : "";
            JLabel lbl = new JLabel((idx + 1) + ".  " + ts + "  [" + rec.tag + "]" + note);
            lbl.setOpaque(true);
            lbl.setBackground(sel ? list.getSelectionBackground() : list.getBackground());
            lbl.setForeground(sel ? list.getSelectionForeground() : list.getForeground());
            lbl.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            return lbl;
        });

        backupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                BackupRecord sel = backupList.getSelectedValue();
                if (sel != null) detail.setText(buildDetail(sel));
            }
        });

        JButton refresh    = new JButton("Refresh");
        JButton createBtn  = new JButton("Create backup");
        refresh.addActionListener(e -> refresh());
        createBtn.addActionListener(e -> createBackup());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(refresh);
        toolbar.add(new JLabel("Note:"));
        toolbar.add(noteField);
        toolbar.add(createBtn);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            new JScrollPane(backupList), new JScrollPane(detail));
        split.setResizeWeight(0.6);

        add(toolbar, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    private String buildDetail(BackupRecord rec) {
        return "File:      " + rec.sourcePath + "\n"
            + "Timestamp: " + rec.timestamp + "\n"
            + "Tag:       " + rec.tag + "\n"
            + (rec.note != null && !rec.note.isBlank() ? "Note:      " + rec.note + "\n" : "")
            + "OS:        " + rec.os + "\n\n"
            + "System PATH (" + rec.systemPath.size() + " entries):\n"
            + String.join("\n", rec.systemPath.stream().map(p -> "  " + p).toList()) + "\n\n"
            + "User PATH (" + rec.userPath.size() + " entries):\n"
            + String.join("\n", rec.userPath.stream().map(p -> "  " + p).toList());
    }

    private void createBackup() {
        String note = noteField.getText().strip();
        statusBar.setText("Creating backup…");
        SwingWorker<Optional<java.nio.file.Path>, Void> worker = new SwingWorker<>() {
            @Override protected Optional<java.nio.file.Path> doInBackground() throws Exception {
                var snap = state.snapshot();
                return BackupStore.create(snap, state.backupsDir(), "manual", note, false);
            }
            @Override protected void done() {
                try {
                    Optional<java.nio.file.Path> result = get();
                    if (result.isPresent()) {
                        statusBar.setText("Backup created: " + result.get().getFileName());
                        noteField.setText("");
                        refresh();
                    } else {
                        statusBar.setText("No change — backup skipped.");
                    }
                } catch (Exception ex) {
                    statusBar.setText("Error: " + ex.getCause().getMessage());
                }
            }
        };
        worker.execute();
    }

    void refresh() {
        statusBar.setText("Loading backups…");
        SwingWorker<List<BackupRecord>, Void> worker = new SwingWorker<>() {
            @Override protected List<BackupRecord> doInBackground() throws Exception {
                return state.backups();
            }
            @Override protected void done() {
                try {
                    List<BackupRecord> records = get();
                    listModel.clear();
                    records.forEach(listModel::addElement);
                    statusBar.setText("Backups: " + records.size() + " found.");
                } catch (Exception ex) {
                    statusBar.setText("Error: " + ex.getCause().getMessage());
                }
            }
        };
        worker.execute();
    }
}
