package com.chatbot.dto;

/**
 * Request body for sending a chat message
 */
public class ChatRequest {
    private String message;
    private String mode; // NORMAL | FANTASY | COMPANION | FLIRT

    public ChatRequest() {}

    public String getMessage() { return message; }
    public String getMode() { return mode; }

    public void setMessage(String message) { this.message = message; }
    public void setMode(String mode) { this.mode = mode; }
}

