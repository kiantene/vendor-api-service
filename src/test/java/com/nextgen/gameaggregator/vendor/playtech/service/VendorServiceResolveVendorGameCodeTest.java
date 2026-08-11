package com.nextgen.gameaggregator.vendor.playtech.service;

import com.nextgen.gameaggregator.vendor.playtech.dto.LiveTableDetailsDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers GA-14842: for live-table bets, Playtech's top-level gameCodeName is a coarser
 * game-type code (e.g. "ubal") that doesn't even share a prefix with liveTableDetails.launchAlias
 * (e.g. "bal_speedbac2" - notice it's not "ubal_speedbac2"), so neither alone is the vendorGameCode
 * we stored on the GameSession - it's the two concatenated as "gameCodeName;launchAlias".
 * Electronic games have no liveTableDetails at all, so gameCodeName is the vendorGameCode as-is.
 */
class VendorServiceResolveVendorGameCodeTest {

    @Test
    void electronicGame_noLiveTableDetails_usesGameCodeName() {
        String result = VendorService.resolveVendorGameCode("bj", null);

        assertThat(result).isEqualTo("bj");
    }

    @Test
    void liveGame_concatenatesGameCodeNameAndLaunchAlias() {
        LiveTableDetailsDto liveTableDetails = new LiveTableDetailsDto();
        liveTableDetails.setLaunchAlias("bal_speedbac2");
        liveTableDetails.setTableId("12510");

        String result = VendorService.resolveVendorGameCode("ubal", liveTableDetails);

        assertThat(result).isEqualTo("ubal;bal_speedbac2");
    }

    @Test
    void liveTableDetailsPresent_butLaunchAliasBlank_fallsBackToGameCodeName() {
        LiveTableDetailsDto liveTableDetails = new LiveTableDetailsDto();
        liveTableDetails.setLaunchAlias("  ");

        String result = VendorService.resolveVendorGameCode("ubal", liveTableDetails);

        assertThat(result).isEqualTo("ubal");
    }

    @Test
    void liveTableDetailsPresent_butLaunchAliasNull_fallsBackToGameCodeName() {
        LiveTableDetailsDto liveTableDetails = new LiveTableDetailsDto();

        String result = VendorService.resolveVendorGameCode("ubal", liveTableDetails);

        assertThat(result).isEqualTo("ubal");
    }
}
