package com.nextgen.gameaggregator.vendor.koolbet.api.v2.betandresult;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bet History "Bet Time is 0" fix: wagersTime (unix seconds from the vendor) was never mapped to
 * vendorBetTime/vendorSettleTime, so the enricher left betTime null and bet history stored 0 -
 * matches v1's BetNSettleDto.getVendorBetTime()/getVendorSettleTime() (wagersTime * 1000).
 */
class BetAndResultRequestMapperTest {

    private final BetAndResultRequestMapper mapper = new BetAndResultRequestMapper();

    @Test
    void toInternal_mapsWagersTimeToVendorBetAndSettleTime() {
        BetAndResultRequest request = new BetAndResultRequest();
        request.setToken("token-1");
        request.setGame(BigDecimal.valueOf(202));
        request.setRound(BigInteger.valueOf(7670858410239332710L));
        request.setWagersTime(1786010901L);
        request.setBetAmount(BigDecimal.TEN);
        request.setWinloseAmount(BigDecimal.ZERO);
        request.setUsername("player-1");

        BetResultContext context = mapper.toInternal(request);

        assertThat(context.getVendorBetTime()).isEqualTo(1786010901L * 1000);
        assertThat(context.getVendorSettleTime()).isEqualTo(1786010901L * 1000);
    }
}
