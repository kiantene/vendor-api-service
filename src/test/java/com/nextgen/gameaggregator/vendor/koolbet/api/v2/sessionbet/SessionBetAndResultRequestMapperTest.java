package com.nextgen.gameaggregator.vendor.koolbet.api.v2.sessionbet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bet History "Bet Time is 0" fix: wagersTime (unix seconds from the vendor) was never mapped -
 * BetContext.timestamp is this leg's "Vendor bet time" field (BetResultContext uses
 * vendorBetTime/vendorSettleTime instead - see the sibling sessionresult mapper).
 */
class SessionBetAndResultRequestMapperTest {

    private final SessionBetAndResultRequestMapper mapper = new SessionBetAndResultRequestMapper();

    @Test
    void toInternal_mapsWagersTimeToTimestamp() {
        SessionBetAndResultRequest request = new SessionBetAndResultRequest();
        request.setToken("token-1");
        request.setGame(BigDecimal.valueOf(202));
        request.setRound(BigInteger.valueOf(7670858410239332710L));
        request.setSessionId(BigInteger.valueOf(123));
        request.setWagersTime(1786010901L);
        request.setBetAmount(BigDecimal.TEN);
        request.setUsername("player-1");

        BetContext context = mapper.toInternal(request);

        assertThat(context.getTimestamp()).isEqualTo(1786010901L * 1000);
    }
}
