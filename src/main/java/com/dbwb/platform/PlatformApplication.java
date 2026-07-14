package com.dbwb.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Dynamic Business Website Builder platform backend.
 * Scheduling is enabled because several MVP business rules are time-driven
 * (subscription grace-period expiry, temporary item unavailability windows,
 * trash retention cleanup, suspension auto-reactivation).
 */
@SpringBootApplication
@EnableScheduling
public class PlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
