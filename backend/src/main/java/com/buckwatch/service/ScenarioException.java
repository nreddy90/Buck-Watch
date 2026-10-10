package com.buckwatch.service;

import java.util.List;

public class ScenarioException extends RuntimeException {
    private final String code;
    private final List<String> choices;
    public ScenarioException(String code, String message) { this(code, message, List.of()); }
    public ScenarioException(String code, String message, List<String> choices) {
        super(message); this.code = code; this.choices = choices;
    }
    public String code() { return code; }
    public List<String> choices() { return choices; }
}
