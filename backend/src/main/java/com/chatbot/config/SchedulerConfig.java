package com.chatbot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's @Scheduled annotation support.
 * Required for SchedulerService to work.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // No additional configuration needed.
    // Just enabling scheduling for the whole application.
}

