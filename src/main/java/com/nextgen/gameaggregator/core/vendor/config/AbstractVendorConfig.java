package com.nextgen.gameaggregator.core.vendor.config;

public abstract class AbstractVendorConfig implements VendorConfig {

    public static final int DEFAULT_TIMEOUT = 4000;

    @Override
    public int getTimeoutInMillis() {
        return DEFAULT_TIMEOUT;
    }

    @Override
    public boolean isNewFramework() {
        return true;
    }

    @Override
    public boolean isTransactionHistoryEnabled() {
        return false;
    }

    @Override
    public boolean isWalletServiceLegacyEnabled() {
        return true;
    }
}
