package com.matthewdeanmartin.pathkeeper.backup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.matthewdeanmartin.pathkeeper.config.AppConfig;
import com.matthewdeanmartin.pathkeeper.config.ConfigLoader;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

public class BackupStore {

    private static final DateTimeFormatter FILENAME_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss").withZone(ZoneOffset.UTC);

    private static final ObjectMapper MAPPER = buildMapper();

    private static ObjectMapper buildMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /** Create a new backup. Returns the path written, or empty if skipped (no change). */
    public static Optional<Path> create(
        Snapshot snapshot,
        Path backupDir,
        String tag,
        String note,
        boolean force
    ) throws IOException {
        if (tag == null || tag.isBlank()) tag = "manual";
        Files.createDirectories(backupDir);

        Optional<BackupRecord> latest = loadLatest(backupDir);
        if (!force && latest.isPresent() && snapshotsEqual(latest.get().toSnapshot(), snapshot)) {
            return Optional.empty();
        }

        BackupRecord record = new BackupRecord();
        record.version = BackupRecord.FORMAT_VERSION;
        record.timestamp = Instant.now();
        record.hostname = safeHostname();
        record.os = System.getProperty("os.name", "unknown");
        record.tag = tag;
        record.note = note != null ? note : "";
        record.systemPath = new ArrayList<>(snapshot.systemPath());
        record.userPath = new ArrayList<>(snapshot.userPath());
        record.systemPathRaw = snapshot.systemPathRaw();
        record.userPathRaw = snapshot.userPathRaw();
        record.systemEnvVars = new LinkedHashMap<>(snapshot.systemEnvVars());
        record.userEnvVars = new LinkedHashMap<>(snapshot.userEnvVars());

        Path dest = uniquePath(backupDir, record.timestamp, tag);
        MAPPER.writeValue(dest.toFile(), record);

        prune(backupDir);
        return Optional.of(dest);
    }

    public static BackupRecord load(Path path) throws IOException {
        BackupRecord record = MAPPER.readValue(path.toFile(), BackupRecord.class);
        record.sourcePath = path.toString();
        if (record.systemEnvVars == null) record.systemEnvVars = new LinkedHashMap<>();
        if (record.userEnvVars == null) record.userEnvVars = new LinkedHashMap<>();
        if (record.systemPath == null) record.systemPath = new ArrayList<>();
        if (record.userPath == null) record.userPath = new ArrayList<>();
        return record;
    }

    public static Optional<BackupRecord> loadLatest(Path backupDir) throws IOException {
        List<BackupRecord> records = list(backupDir);
        return records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
    }

    /** Returns records newest-first. */
    public static List<BackupRecord> list(Path backupDir) throws IOException {
        if (!Files.isDirectory(backupDir)) return List.of();

        List<Path> jsonFiles;
        try (Stream<Path> stream = Files.list(backupDir)) {
            jsonFiles = stream
                .filter(p -> p.toString().endsWith(".json"))
                .sorted(Comparator.reverseOrder())
                .toList();
        }

        List<BackupRecord> records = new ArrayList<>();
        for (Path p : jsonFiles) {
            records.add(load(p));
        }
        return records;
    }

    /**
     * Resolve a backup by identifier:
     *   ""       → latest
     *   "1"      → index 1 (1-based, newest-first)
     *   filename prefix or exact path
     */
    public static BackupRecord resolve(String identifier, Path backupDir) throws IOException {
        if (identifier == null || identifier.isBlank()) {
            return loadLatest(backupDir)
                .orElseThrow(() -> new IllegalArgumentException("No backups available"));
        }

        try {
            int idx = Integer.parseInt(identifier);
            List<BackupRecord> records = list(backupDir);
            if (idx < 1 || idx > records.size()) {
                throw new IllegalArgumentException("Backup not found: " + identifier);
            }
            return records.get(idx - 1);
        } catch (NumberFormatException ignored) {
        }

        Path asPath = Path.of(identifier);
        if (Files.isRegularFile(asPath)) {
            return load(asPath);
        }

        for (BackupRecord r : list(backupDir)) {
            String name = Path.of(r.sourcePath).getFileName().toString();
            String stem = name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
            if (name.equals(identifier) || stem.startsWith(identifier)) {
                return r;
            }
        }

        throw new IllegalArgumentException("Backup not found: " + identifier);
    }

    public static String contentHash(BackupRecord record) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("os", record.os);
            payload.put("system_path", record.systemPath);
            payload.put("user_path", record.userPath);
            payload.put("system_path_raw", record.systemPathRaw);
            payload.put("user_path_raw", record.userPathRaw);
            payload.put("system_env_vars", record.systemEnvVars);
            payload.put("user_env_vars", record.userEnvVars);

            byte[] bytes = MAPPER.writeValueAsBytes(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.substring(0, 12);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void prune(Path backupDir) throws IOException {
        AppConfig cfg = ConfigLoader.load();
        List<BackupRecord> records = list(backupDir);

        Set<String> keep = new LinkedHashSet<>();
        List<BackupRecord> autos = records.stream().filter(r -> "auto".equals(r.tag)).toList();
        List<BackupRecord> manuals = records.stream().filter(r -> "manual".equals(r.tag)).toList();
        List<BackupRecord> others = records.stream()
            .filter(r -> !"auto".equals(r.tag) && !"manual".equals(r.tag)).toList();

        autos.stream().limit(cfg.maxAutoBackups()).forEach(r -> keep.add(r.sourcePath));
        manuals.stream().limit(cfg.maxManualBackups()).forEach(r -> keep.add(r.sourcePath));

        int remaining = Math.max(0, cfg.maxBackups() - keep.size());
        others.stream().limit(remaining).forEach(r -> keep.add(r.sourcePath));

        for (BackupRecord r : records) {
            if (!keep.contains(r.sourcePath)) {
                Files.deleteIfExists(Path.of(r.sourcePath));
            }
        }
    }

    private static Path uniquePath(Path dir, Instant timestamp, String tag) {
        String base = FILENAME_FMT.format(timestamp) + "_" + tag;
        Path candidate = dir.resolve(base + ".json");
        for (int i = 1; Files.exists(candidate); i++) {
            candidate = dir.resolve(base + String.format("-%02d", i) + ".json");
        }
        return candidate;
    }

    private static boolean snapshotsEqual(Snapshot a, Snapshot b) {
        return a.systemPath().equals(b.systemPath())
            && a.userPath().equals(b.userPath())
            && a.systemPathRaw().equals(b.systemPathRaw())
            && a.userPathRaw().equals(b.userPathRaw())
            && a.systemEnvVars().equals(b.systemEnvVars())
            && a.userEnvVars().equals(b.userEnvVars());
    }

    private static String safeHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
