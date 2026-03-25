package com.communityplatform.community.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * A minimal health-check endpoint.
 * Hit GET http://localhost:8080/api/health to confirm the app started correctly.
 *
 * You can delete this class once the real controllers are in place.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "message", "Community Platform is running"
        );
    }
}
