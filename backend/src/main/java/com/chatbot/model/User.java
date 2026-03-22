package com.chatbot.model;

import java.time.LocalDateTime;

/**
 * Represents a user in the system.
 * Maps to a row in users.xlsx
 */
public class User {
    private String id;
    private String username;
    private String email;
    private String password;  // BCrypt hashed
    private int tokens;
    private LocalDateTime lastResetTime;

    public User() {}

    public User(String id, String username, String email, String password, int tokens, LocalDateTime lastResetTime) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.tokens = tokens;
        this.lastResetTime = lastResetTime;
    }

    // Getters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public int getTokens() { return tokens; }
    public LocalDateTime getLastResetTime() { return lastResetTime; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setTokens(int tokens) { this.tokens = tokens; }
    public void setLastResetTime(LocalDateTime lastResetTime) { this.lastResetTime = lastResetTime; }

    @Override
    public String toString() {
        return "User{id='" + id + "', username='" + username + "', email='" + email + "', tokens=" + tokens + "}";
    }
}

