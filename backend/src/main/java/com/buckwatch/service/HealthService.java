package com.buckwatch.service;

import com.buckwatch.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public HealthResponse getHealth() {
        return new HealthResponse("ok", "buckwatch-backend");
    }
}
