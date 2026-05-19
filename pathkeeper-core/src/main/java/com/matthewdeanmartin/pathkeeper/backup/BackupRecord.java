package com.matthewdeanmartin.pathkeeper.backup;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class BackupRecord {

    public static final int FORMAT_VERSION = 1;

    @JsonProperty("version")
    public int version = FORMAT_VERSION;

    @JsonProperty("timestamp")
    public Instant timestamp;

    @JsonProperty("hostname")
    public String hostname;

    @JsonProperty("os")
    public String os;

    @JsonProperty("tag")
    public String tag;

    @JsonProperty("note")
    public String note;

    @JsonProperty("system_path")
    public List<String> systemPath;

    @JsonProperty("user_path")
    public List<String> userPath;

    @JsonProperty("system_path_raw")
    public String systemPathRaw;

    @JsonProperty("user_path_raw")
    public String userPathRaw;

    @JsonProperty("system_env_vars")
    public Map<String, String> systemEnvVars;

    @JsonProperty("user_env_vars")
    public Map<String, String> userEnvVars;

    /** Not serialized — set after loading so callers know the file path. */
    @JsonIgnore
    public String sourcePath;

    public Snapshot toSnapshot() {
        return new Snapshot(
            List.copyOf(systemPath != null ? systemPath : List.of()),
            List.copyOf(userPath != null ? userPath : List.of()),
            systemPathRaw != null ? systemPathRaw : "",
            userPathRaw != null ? userPathRaw : "",
            systemEnvVars != null ? Map.copyOf(systemEnvVars) : Map.of(),
            userEnvVars != null ? Map.copyOf(userEnvVars) : Map.of()
        );
    }
}
