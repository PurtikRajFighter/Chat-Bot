package com.chatbot.model;

import java.time.LocalDateTime;

/**
 * Represents a single chat message.
 * Maps to a row in chats.xlsx
 * role: "user" or "assistant"
 */
public class Chat {
    private String id;
    private String userId;
    private String mode;       // NORMAL | FANTASY | COMPANION | FLIRT
    private String message;    // The actual message content
    private String role;       // "user" or "assistant"
    private LocalDateTime timestamp;

    public Chat() {}

    public Chat(String id, String userId, String mode, String message, String role, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.mode = mode;
        this.message = message;
        this.role = role;
        this.timestamp = timestamp;
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getMode() { return mode; }
    public String getMessage() { return message; }
    public String getRole() { return role; }
    public LocalDateTime getTimestamp() { return timestamp; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setMode(String mode) { this.mode = mode; }
    public void setMessage(String message) { this.message = message; }
    public void setRole(String role) { this.role = role; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

