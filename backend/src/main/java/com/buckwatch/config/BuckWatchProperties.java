package com.buckwatch.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "buckwatch")
@Validated
public class BuckWatchProperties {

    private final Ml ml = new Ml();

    public Ml getMl() {
        return ml;
    }

    public static class Ml {
        @NotBlank
        private String baseUrl = "http://localhost:8000";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
