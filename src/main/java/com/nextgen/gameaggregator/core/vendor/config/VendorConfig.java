package com.nextgen.gameaggregator.core.vendor.config;

import com.nextgen.gameaggregator.core.vendor.VendorComponent;

public interface VendorConfig extends VendorComponent {
    int getTimeoutInMillis();

    boolean isNewFramework();

    boolean isTransactionHistoryEnabled();

    boolean isWalletServiceLegacyEnabled();
}
