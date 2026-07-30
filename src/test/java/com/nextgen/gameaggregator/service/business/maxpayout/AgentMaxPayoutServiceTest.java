package com.nextgen.gameaggregator.service.business.maxpayout;

import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.service.AgentDataService;
import com.nextgen.gameaggregator.enums.Features;
import com.nextgen.gameaggregator.service.data.AgentPayoutSettingDataService;
import com.nextgen.gameaggregator.service.data.VendorFeatureDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentMaxPayoutServiceTest {

    private static final int VENDOR_ID = 134;
    private static final int AGENT_ID = 1116;
    private static final int MASTER_AGENT_ID = 1;
    private static final int GAME_CATEGORY_ID = 1;
    private static final int CURRENCY_ID = 7;
    private static final BigDecimal RATE_ONE = BigDecimal.ONE;

    private AgentDataService agentService;
    private AgentPayoutSettingDataService payoutSettingsDataService;
    private VendorFeatureDataService vendorFeatureDataService;
    private AgentMaxPayoutService service;

    @BeforeEach
    void setUp() {
        agentService = mock(AgentDataService.class);
        payoutSettingsDataService = mock(AgentPayoutSettingDataService.class);
        vendorFeatureDataService = mock(VendorFeatureDataService.class);
        service = new AgentMaxPayoutService(agentService, payoutSettingsDataService, vendorFeatureDataService);

        Agent agent = mock(Agent.class);
        when(agent.getId()).thenReturn(AGENT_ID);
        when(agent.getMasterAgentId()).thenReturn(MASTER_AGENT_ID);
        when(agentService.get(AGENT_ID)).thenReturn(agent);
    }

    private void featureEnabled(boolean enabled) {
        when(vendorFeatureDataService.isVendorEnabled(eq(Features.AGENT_MAX_PAYOUT), eq(VENDOR_ID))).thenReturn(enabled);
    }

    private void capConfig(BigDecimal cap) {
        when(payoutSettingsDataService.getMaxPayoutAmount(
                eq(MASTER_AGENT_ID), eq(AGENT_ID), eq(VENDOR_ID), eq(GAME_CATEGORY_ID), eq(CURRENCY_ID)))
                .thenReturn(cap);
    }

    private CapRequest req(String bet, String win, String jackpot) {
        return new CapRequest(AGENT_ID, VENDOR_ID, GAME_CATEGORY_ID, CURRENCY_ID,
                new BigDecimal(bet), new BigDecimal(win), new BigDecimal(jackpot));
    }

    /** OVI-2391: win above cap is capped; win_loss recomputed against the (uncapped) bet. */
    @Test
    void capsWinAboveCap() {
        featureEnabled(true);
        capConfig(new BigDecimal("2000"));

        ResultAmounts r = service.applyPayoutCap(req("2167", "2100", "0"), RATE_ONE);

        assertTrue(r.capped());
        assertEquals(0, r.cappedWin().compareTo(new BigDecimal("2000")));
        assertEquals(0, r.cappedWinLoss().compareTo(new BigDecimal("-167")));
    }

    @Test
    void doesNotCapWhenWithinCap() {
        featureEnabled(true);
        capConfig(new BigDecimal("5000"));

        ResultAmounts r = service.applyPayoutCap(req("2167", "2100", "0"), RATE_ONE);

        assertFalse(r.capped());
        assertEquals(0, r.cappedWin().compareTo(new BigDecimal("2100")));
    }

    @Test
    void capsJackpotIndependentlyOfWin() {
        featureEnabled(true);
        capConfig(new BigDecimal("2000"));

        // win under cap, jackpot over cap
        ResultAmounts r = service.applyPayoutCap(req("100", "1000", "3000"), RATE_ONE);

        assertTrue(r.capped());
        assertEquals(0, r.cappedWin().compareTo(new BigDecimal("1000")));
        assertEquals(0, r.cappedJackpot().compareTo(new BigDecimal("2000")));
    }

    /**
     * Behavioral note vs the old {@code shouldApplyCap}: the old guard returned false if EITHER
     * win or jackpot was null, so a null jackpot bypassed capping entirely. The new core normalizes
     * null→0, so win &gt; cap is capped regardless of a null jackpot. This is intentional (the old
     * bypass was a latent bug) — documented here.
     */
    @Test
    void capsWin_whenJackpotNull() {
        featureEnabled(true);
        capConfig(new BigDecimal("2000"));

        CapRequest r = new CapRequest(AGENT_ID, VENDOR_ID, GAME_CATEGORY_ID, CURRENCY_ID,
                new BigDecimal("2167"), new BigDecimal("2100"), null);
        ResultAmounts res = service.applyPayoutCap(r, RATE_ONE);

        assertTrue(res.capped());
        assertEquals(0, res.cappedWin().compareTo(new BigDecimal("2000")));
        assertEquals(0, res.cappedJackpot().compareTo(BigDecimal.ZERO)); // null jackpot → 0
    }

    @Test
    void noOpWhenFeatureDisabled() {
        featureEnabled(false);
        capConfig(new BigDecimal("2000")); // even with config present

        ResultAmounts r = service.applyPayoutCap(req("2167", "2100", "0"), RATE_ONE);

        assertFalse(r.capped());
        assertEquals(0, r.cappedWin().compareTo(new BigDecimal("2100")));
    }

    @Test
    void noOpWhenNoCapConfig() {
        featureEnabled(true);
        capConfig(null);

        ResultAmounts r = service.applyPayoutCap(req("2167", "2100", "0"), RATE_ONE);

        assertFalse(r.capped());
        assertEquals(0, r.cappedWin().compareTo(new BigDecimal("2100")));
    }

    /** Cap config is in operator currency; a non-unit toVendorRate converts it into vendor units. */
    @Test
    void convertsCapConfigByToVendorRate() {
        featureEnabled(true);
        capConfig(new BigDecimal("2")); // operator-currency cap

        // toVendorRate = 1000 -> vendor-unit cap = 2000; vendor win 2100 -> capped 2000
        ResultAmounts r = service.applyPayoutCap(req("2167", "2100", "0"), new BigDecimal("1000"));

        assertTrue(r.capped());
        assertEquals(0, r.cappedWin().compareTo(new BigDecimal("2000")));
    }
}
