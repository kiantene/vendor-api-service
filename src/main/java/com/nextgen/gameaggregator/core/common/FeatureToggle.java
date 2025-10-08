package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.vendor.Vendors;

import java.util.List;

public class FeatureToggle {
    private static final List<Integer> refactoredBetHistoryList = List.of(
            Vendors.EZUGI.getId()
    );

    private FeatureToggle() {}

    public static boolean useRefactoredPublishBetHistory(Integer vendorId) {
        if (vendorId == null) return false;

        if (refactoredBetHistoryList.contains(vendorId)) return true;

        int fromVendorOnwards = Vendors.AVIATOR_STUDIO.getId();

        return Vendors.isNewFramework(vendorId) && vendorId > fromVendorOnwards;
    }
}
