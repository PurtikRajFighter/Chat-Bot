
package com.chatbot.service;

import com.chatbot.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * TokenService — handles all token operations:
 * - Deducting tokens when a message is sent
 * - Adding tokens after a payment
 * - Checking token balance
 * - Token reset (called by SchedulerService)
 */
@Service
public class TokenService {

    @Autowired
    private ExcelService excelService;

    // How many tokens to deduct per message
    @Value("${tokens.cost.per.message}")
    private int costPerMessage;

    // Default tokens given on reset
    @Value("${tokens.reset.amount}")
    private int resetAmount;

    /**
     * Checks if user has enough tokens to send a message.
     * Returns true if tokens > 0, false if blocked.
     */
    public boolean hasTokens(String userId) {
        return getBalance(userId) >= costPerMessage;
    }

    /**
     * Get current token balance for a user.
     */
    public int getBalance(String userId) {
        return excelService.findUserById(userId)
                .map(User::getTokens)
                .orElse(0);
    }

    /**
     * Deducts one message worth of tokens from the user.
     * Returns false if user has insufficient tokens (safe guard).
     */
    public boolean deductToken(String userId) {
        Optional<User> optUser = excelService.findUserById(userId);
        if (optUser.isEmpty()) return false;

        User user = optUser.get();
        if (user.getTokens() < costPerMessage) return false;  // prevent negative tokens

        user.setTokens(user.getTokens() - costPerMessage);
        excelService.updateUser(user);
        return true;
    }

    /**
     * Adds tokens to a user (after payment).
     * @param userId  the user to credit
     * @param amount  number of tokens to add
     */
    public void addTokens(String userId, int amount) {
        Optional<User> optUser = excelService.findUserById(userId);
        if (optUser.isEmpty()) throw new RuntimeException("User not found: " + userId);

        User user = optUser.get();
        user.setTokens(user.getTokens() + amount);
        excelService.updateUser(user);
    }

    /**
     * Resets a user's tokens to the default reset amount.
     * Updates last_reset_time to now.
     * Called by SchedulerService.
     */
    public void resetTokensForUser(User user) {
        user.setTokens(resetAmount);
        user.setLastResetTime(java.time.LocalDateTime.now());
        excelService.updateUser(user);
    }
}

