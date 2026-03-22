package com.chatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * PackageConfig — binds the payment.packages[N] list from application.properties.
 *
 * Each entry maps to one payment package shown in the UI.
 * Add/remove packages purely in application.properties — no code changes needed.
 */
@ConfigurationProperties(prefix = "payment")
public class PackageConfig {

    /** Bound from payment.packages[N].* in application.properties */
    private List<PackageItem> packages = new ArrayList<>();

    public List<PackageItem> getPackages() { return packages; }
    public void setPackages(List<PackageItem> packages) { this.packages = packages; }

    // -------------------------------------------------------------------------
    // Inner class — one token package
    // -------------------------------------------------------------------------
    public static class PackageItem {
        /** Price in rupees (e.g. 10, 25, 50) */
        private int amount;

        /** Display label in the UI (e.g. "Rs.10") */
        private String label = "";

        /** Tokens given at the normal (non-sale) price */
        private int baseTokens;

        /** Whether to show a "Popular" badge on this card */
        private boolean popular = false;

        public int    getAmount()     { return amount; }
        public String getLabel()      { return label; }
        public int    getBaseTokens() { return baseTokens; }
        public boolean isPopular()    { return popular; }

        public void setAmount(int amount)         { this.amount = amount; }
        public void setLabel(String label)         { this.label = label; }
        public void setBaseTokens(int baseTokens) { this.baseTokens = baseTokens; }
        public void setPopular(boolean popular)    { this.popular = popular; }
    }
}

