package com.matthewdeanmartin.pathkeeper.diagnostics;

import java.util.List;

public record DoctorCheck(
    String name,
    String status,
    String detail,
    List<DiagnosticEntry> affected,
    String remediation
) {
    public static final String PASS = "pass";
    public static final String FAIL = "fail";
    public static final String WARN = "warn";

    public static DoctorCheck pass(String name, String detail) {
        return new DoctorCheck(name, PASS, detail, List.of(), "");
    }

    public static DoctorCheck fail(String name, String detail, List<DiagnosticEntry> affected, String remediation) {
        return new DoctorCheck(name, FAIL, detail, affected, remediation);
    }

    public static DoctorCheck warn(String name, String detail, List<DiagnosticEntry> affected, String remediation) {
        return new DoctorCheck(name, WARN, detail, affected, remediation);
    }
}
