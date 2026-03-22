package com.chatbot.model;

import java.time.LocalDateTime;

/**
 * Represents a payment transaction.
 * Maps to a row in transactions.xlsx
 */
public class Transaction {
    private String id;
    private String userId;
    private double amount;        // Amount in INR (rupees)
    private int tokensAdded;      // Tokens credited to user
    private String status;        // created | paid | failed
    private LocalDateTime timestamp;
    private String razorpayOrderId;  // Order ID from Razorpay

    public Transaction() {}

    public Transaction(String id, String userId, double amount, int tokensAdded,
                       String status, LocalDateTime timestamp, String razorpayOrderId) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.tokensAdded = tokensAdded;
        this.status = status;
        this.timestamp = timestamp;
        this.razorpayOrderId = razorpayOrderId;
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public double getAmount() { return amount; }
    public int getTokensAdded() { return tokensAdded; }
    public String getStatus() { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getRazorpayOrderId() { return razorpayOrderId; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setTokensAdded(int tokensAdded) { this.tokensAdded = tokensAdded; }
    public void setStatus(String status) { this.status = status; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
}

