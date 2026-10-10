package com.buckwatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BuckWatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuckWatchApplication.class, args);
    }
}
