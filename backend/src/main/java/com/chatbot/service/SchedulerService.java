package com.chatbot.service;

import com.chatbot.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SchedulerService — background job that runs on a cron schedule.
 *
 * Iterates over all users in users.xlsx and checks if their
 * last_reset_time is older than the configured interval.
 * If so, resets their token balance to the default amount.
 */
@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    @Autowired
    private ExcelService excelService;

    @Autowired
    private TokenService tokenService;

    // Number of hours between token resets per user
    @Value("${tokens.reset.interval.hours}")
    private int resetIntervalHours;

    /**
     * Runs every hour (configurable via scheduler.token.reset.cron).
     * Checks each user's last_reset_time and resets tokens if the interval has passed.
     */
    @Scheduled(cron = "${scheduler.token.reset.cron}")
    public void resetTokensIfDue() {
        log.info("[Scheduler] Running token reset check...");
        List<User> allUsers = excelService.getAllUsers();

        int resetCount = 0;
        for (User user : allUsers) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextReset = user.getLastResetTime().plusHours(resetIntervalHours);

            if (now.isAfter(nextReset)) {
                // Time to reset this user's tokens
                log.info("[Scheduler] Resetting tokens for user: {}", user.getUsername());
                tokenService.resetTokensForUser(user);
                resetCount++;
            }
        }

        log.info("[Scheduler] Token reset complete. {} users reset.", resetCount);
    }
}

