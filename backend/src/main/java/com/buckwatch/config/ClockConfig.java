package com.buckwatch.config;
import org.springframework.context.annotation.*;
import java.time.Clock;
@Configuration
public class ClockConfig { @Bean public Clock clock() { return Clock.systemDefaultZone(); } }
