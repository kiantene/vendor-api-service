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
 * vendorSettleTime, so the enricher left settleTime null and bet history stored 0 - matches v1's
 * BetNSettleDto.getVendorSettleTime() (wagersTime * 1000). betTime is deliberately NOT set here:
 * for session bet+result, betTime always comes from the separate BET-type transaction created by
 * the earlier session bet call (already fixed via SessionBetAndResultRequestMapper.timestamp).
 */
class BetResultRequestMapperTest {

    private final BetResultRequestMapper mapper = new BetResultRequestMapper();

    @Test
    void toInternal_mapsWagersTimeToVendorSettleTime() {
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

        assertThat(context.getVendorSettleTime()).isEqualTo(1786010901L * 1000);
    }
}
