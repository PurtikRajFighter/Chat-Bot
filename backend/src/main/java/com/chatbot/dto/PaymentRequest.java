package com.chatbot.dto;

/**
 * Request body for creating a payment order
 */
public class PaymentRequest {
    private double amount;  // Amount in INR (rupees)
    private String razorpayPaymentId;   // For verification
    private String razorpayOrderId;     // For verification
    private String razorpaySignature;   // For verification

    public PaymentRequest() {}

    public double getAmount() { return amount; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public String getRazorpaySignature() { return razorpaySignature; }

    public void setAmount(double amount) { this.amount = amount; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public void setRazorpaySignature(String razorpaySignature) { this.razorpaySignature = razorpaySignature; }
}

