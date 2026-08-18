package com.visionselect.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * VisionSelect AI backend entry point.
 *
 * <p>Phase 2B established the runnable foundation (config, common,
 * security scaffolding). Phase 2C added the auth module. This phase adds
 * the video-upload module ({@code video}, {@code storage}) - the
 * two-step direct-to-storage upload flow from storage-contract.md.
 * Analysis jobs, results, reports, and the AI-service internal callback
 * remain out of scope.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
