package com.matthewdeanmartin.pathkeeper.gui;

import com.matthewdeanmartin.pathkeeper.backup.BackupStore;
import com.matthewdeanmartin.pathkeeper.backup.Snapshot;
import com.matthewdeanmartin.pathkeeper.dedupe.Deduper;
import com.matthewdeanmartin.pathkeeper.dedupe.DedupeResult;
import com.matthewdeanmartin.pathkeeper.diagnostics.Diagnostics;
import com.matthewdeanmartin.pathkeeper.diagnostics.Scope;
import com.matthewdeanmartin.pathkeeper.writer.PathWriters;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class DedupePanel extends JPanel {

    private final AppState state;
    private final JLabel statusBar;
    private final JComboBox<String> keepBox = new JComboBox<>(new String[]{"first", "last"});
    private final JCheckBox removeInvalidBox = new JCheckBox("Remove invalid entries");
    private final JTextArea previewArea = new JTextArea(10, 50);

    DedupePanel(AppState state, JLabel statusBar) {
        this.state = state;
        this.statusBar = statusBar;
        previewArea.setEditable(false);
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JButton preview = new JButton("Preview");
        JButton apply   = new JButton("Apply");
        preview.addActionListener(e -> preview());
        apply.addActionListener(e -> apply());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(new JLabel("Keep:"));
        toolbar.add(keepBox);
        toolbar.add(removeInvalidBox);
        toolbar.add(preview);
        toolbar.add(apply);

        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(previewArea), BorderLayout.CENTER);
    }

    private record DedupePlan(DedupeResult systemResult, DedupeResult userResult) {}

    private DedupePlan buildPlan(String keep, boolean removeInvalid) throws Exception {
        Snapshot snap = state.snapshot();
        Set<String> preSeen = new HashSet<>();
        DedupeResult sysResult = Deduper.dedupe(snap.systemPath(), state.osName, keep, removeInvalid, preSeen);
        sysResult.cleaned().forEach(e -> preSeen.add(Diagnostics.canonicalizeEntry(e, state.osName)));
        DedupeResult userResult = Deduper.dedupe(snap.userPath(), state.osName, keep, removeInvalid, preSeen);
        return new DedupePlan(sysResult, userResult);
    }

    private void preview() {
        String keep = (String) keepBox.getSelectedItem();
        boolean removeInvalid = removeInvalidBox.isSelected();
        statusBar.setText("Computing dedupe preview…");
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override protected String doInBackground() throws Exception {
                DedupePlan plan = buildPlan(keep, removeInvalid);
                return renderPlan(plan);
            }
            @Override protected void done() {
                try { previewArea.setText(get()); statusBar.setText("Preview ready."); }
                catch (Exception ex) { statusBar.setText("Error: " + ex.getCause().getMessage()); }
            }
        };
        worker.execute();
    }

    private void apply() {
        String keep = (String) keepBox.getSelectedItem();
        boolean removeInvalid = removeInvalidBox.isSelected();
        int confirm = JOptionPane.showConfirmDialog(this,
            "Apply dedupe to PATH?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        statusBar.setText("Applying dedupe…");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                Snapshot current = state.snapshot();
                DedupePlan plan = buildPlan(keep, removeInvalid);
                BackupStore.create(current, state.backupsDir(), "auto", "pre-dedupe", false);
                Snapshot updated = new Snapshot(
                    plan.systemResult().cleaned(), plan.userResult().cleaned(),
                    PathWriters.joinPath(plan.systemResult().cleaned(), state.osName),
                    PathWriters.joinPath(plan.userResult().cleaned(), state.osName),
                    current.systemEnvVars(), current.userEnvVars());
                PathWriters.writeChanged(PathWriters.create(state.varName), current, updated, Scope.ALL);
                return null;
            }
            @Override protected void done() {
                try { get(); statusBar.setText("Dedupe applied."); preview(); }
                catch (Exception ex) { statusBar.setText("Error: " + ex.getCause().getMessage()); }
            }
        };
        worker.execute();
    }

    private static String renderPlan(DedupePlan plan) {
        StringBuilder sb = new StringBuilder();
        appendScope(sb, "System", plan.systemResult());
        appendScope(sb, "User", plan.userResult());
        int totalRemoved = plan.systemResult().removedDuplicates().size()
            + plan.systemResult().removedInvalid().size()
            + plan.userResult().removedDuplicates().size()
            + plan.userResult().removedInvalid().size();
        if (totalRemoved == 0) sb.append("No duplicates or invalid entries found.\n");
        return sb.toString();
    }

    private static void appendScope(StringBuilder sb, String label, DedupeResult r) {
        if (!r.removedDuplicates().isEmpty()) {
            sb.append("Duplicates removed from ").append(label).append(":\n");
            r.removedDuplicates().forEach(e -> sb.append("  - ").append(e).append("\n"));
        }
        if (!r.removedInvalid().isEmpty()) {
            sb.append("Invalid entries removed from ").append(label).append(":\n");
            r.removedInvalid().forEach(e -> sb.append("  - ").append(e).append("\n"));
        }
    }

    void refresh() { preview(); }
}
