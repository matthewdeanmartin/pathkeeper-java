package com.matthewdeanmartin.pathkeeper.selfcheck;

public record SelfcheckResult(String name, String status, String detail, String remediation) {
    public static final String PASS = "pass";
    public static final String FAIL = "fail";
    public static final String WARN = "warn";

    public static SelfcheckResult pass(String name, String detail) {
        return new SelfcheckResult(name, PASS, detail, "");
    }
    public static SelfcheckResult fail(String name, String detail, String remediation) {
        return new SelfcheckResult(name, FAIL, detail, remediation);
    }
    public static SelfcheckResult warn(String name, String detail, String remediation) {
        return new SelfcheckResult(name, WARN, detail, remediation);
    }
}
