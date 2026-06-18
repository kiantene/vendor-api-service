package com.nextgen.gameaggregator.vendor.ezugi.api.v2;

import com.nextgen.gameaggregator.core.engine.operator.wallet.result.OperatorBetResultRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RawBetDetailGaBetIdResolverTest {

    @Test
    void resolvesBetIdFromJsonString() {
        assertThat(RawBetDetailGaBetIdResolver.resolve("{\"betId\":\"ga-bet-1\"}"))
                .contains("ga-bet-1");
    }

    @Test
    void resolvesBetIdFromMap() {
        assertThat(RawBetDetailGaBetIdResolver.resolve(Map.of("betId", "ga-bet-2")))
                .contains("ga-bet-2");
    }

    @Test
    void resolvesBetIdFromOperatorRequestObject() {
        OperatorBetResultRequest request = new OperatorBetResultRequest();
        request.setBetId("ga-bet-3");

        assertThat(RawBetDetailGaBetIdResolver.resolve(request))
                .contains("ga-bet-3");
    }

    @Test
    void returnsEmptyWhenBetIdUnavailable() {
        assertThat(RawBetDetailGaBetIdResolver.resolve("{\"transactionId\":\"txn-1\"}"))
                .isEmpty();
        assertThat(RawBetDetailGaBetIdResolver.resolve("not-json"))
                .isEmpty();
        assertThat(RawBetDetailGaBetIdResolver.resolve(null))
                .isEmpty();
    }
}
