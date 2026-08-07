package com.nextgen.gameaggregator.vendor.koolbet.api.v2.sessionresult;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.vendor.koolbet.api.v2.sessionbet.SessionBetAndResultRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bet History "Bet Time is 0" fix: wagersTime (unix seconds from the vendor) was never mapped to
 * vendorBetTime/vendorSettleTime, so the enricher left betTime null and bet history stored 0 -
 * matches v1's BetNSettleDto.getVendorBetTime()/getVendorSettleTime() (wagersTime * 1000).
 */
class BetResultRequestMapperTest {

    private final BetResultRequestMapper mapper = new BetResultRequestMapper();

    @Test
    void toInternal_mapsWagersTimeToVendorBetAndSettleTime() {
        SessionBetAndResultRequest request = new SessionBetAndResultRequest();
        request.setToken("token-1");
        request.setGame(BigDecimal.valueOf(202));
        request.setRound(BigInteger.valueOf(7670858410239332710L));
        request.setSessionId(BigInteger.valueOf(123));
        request.setBetOrder(List.of("bet-order-1"));
        request.setWagersTime(1786010901L);
        request.setWinloseAmount(BigDecimal.ZERO);
        request.setUsername("player-1");
        request.setIsOver(true);

        BetResultContext context = mapper.toInternal(request);

        assertThat(context.getVendorBetTime()).isEqualTo(1786010901L * 1000);
        assertThat(context.getVendorSettleTime()).isEqualTo(1786010901L * 1000);
    }
}
