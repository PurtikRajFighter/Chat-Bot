package com.chatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * DiscountConfig — binds all payment.discount.* settings from application.properties.
 *
 * Per-package extras use:  payment.discount.packageExtras[N].extra=<tokens>
 * (Deliberately named 'packageExtras' to avoid clashing with 'payment.packages[N]'.)
 */
@ConfigurationProperties(prefix = "payment.discount")
public class DiscountConfig {

    private boolean enabled = false;
    private String  label   = "Sale";
    private int     globalBonusPercent = 0;

    /**
     * Bound from payment.discount.packageExtras[N].extra
     * Index N must match payment.packages[N] in PackageConfig.
     */
    private List<PackageExtra> packageExtras = new ArrayList<>();

    public boolean isEnabled()                        { return enabled; }
    public String  getLabel()                         { return label; }
    public int     getGlobalBonusPercent()            { return globalBonusPercent; }
    public List<PackageExtra> getPackageExtras()      { return packageExtras; }

    public void setEnabled(boolean enabled)                       { this.enabled = enabled; }
    public void setLabel(String label)                            { this.label = label; }
    public void setGlobalBonusPercent(int globalBonusPercent)     { this.globalBonusPercent = globalBonusPercent; }
    public void setPackageExtras(List<PackageExtra> packageExtras){ this.packageExtras = packageExtras; }

    /** Returns extra tokens for package at index i, or 0 if index is out of range. */
    public int getExtraTokensFor(int i) {
        if (i >= 0 && i < packageExtras.size()) {
            return packageExtras.get(i).getExtra();
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    public static class PackageExtra {
        private int extra = 0;

        public int  getExtra()          { return extra; }
        public void setExtra(int extra) { this.extra = extra; }
    }
}
