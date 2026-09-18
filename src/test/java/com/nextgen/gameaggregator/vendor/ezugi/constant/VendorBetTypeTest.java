package com.nextgen.gameaggregator.vendor.ezugi.constant;

import com.nextgen.gameaggregator.enums.BetType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VendorBetTypeTest {

    @Test
    void toInternalBetType_bustIt_mapsToBustItSideBetType() {
        assertEquals(BetType.BUST_IT, VendorBetType.toInternalBetType("BustIt"));
    }

    @Test
    void fromVendorValue_bustIt_resolvesToBustItEntry() {
        assertEquals(VendorBetType.BUST_IT, VendorBetType.fromVendorValue("BustIt"));
    }

    @Test
    void toInternalBetType_unrecognisedName_stillDefaultsToNormalBet() {
        // Documents the intended fallback contract itself: an unmapped bet name must keep
        // defaulting to NORMAL_BET rather than throwing, so newly-added mappings never change
        // behaviour for names the vendor hasn't started sending yet.
        assertEquals(BetType.NORMAL_BET, VendorBetType.toInternalBetType("SomeFutureSideBet"));
    }
}
