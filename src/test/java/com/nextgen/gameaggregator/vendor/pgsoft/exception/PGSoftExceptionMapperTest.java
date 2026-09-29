package com.nextgen.gameaggregator.vendor.pgsoft.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.exception.DuplicateBetException;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.ResponseCodes;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Promo payout answers its own duplicates in PGSoftPromoPayoutService, where the request is still in
 * scope — see PGSoftPromoPayoutServiceTest. What is left here is the fallback for a duplicate arriving
 * by any other route, which must not invent a response it cannot complete.
 */
class PGSoftExceptionMapperTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final PGSoftExceptionMapper mapper = new PGSoftExceptionMapper();

    private static String bodyOf(VendorErrorResponse response) throws JsonProcessingException {
        return JSON.writeValueAsString(response.getBody());
    }

    /**
     * Even holding a completed record, the mapper cannot reach real_transfer_amount or updated_time —
     * PG SOFT validates the first against what it sent — so it asks for the request again instead of
     * answering with a partial success body, which is the shape that broke free-round resumption.
     */
    @Test
    void asksForTheRequestAgainRatherThanAnswerADuplicateItCannotComplete() throws Exception {
        RequestIdempotentLog completed = new RequestIdempotentLog();
        completed.setCurrency("BRL");
        completed.setBalance(new BigDecimal("128.44"));

        String body = bodyOf(mapper.onDuplicateRequest(new DuplicateRequestException("already processed", completed)));

        assertThat(body)
                .contains("\"data\":null")
                .contains("\"code\":\"" + ResponseCodes.PLAYER_OPERATION_IN_PROGRESS.getCode() + "\"")
                .doesNotContain("balance_amount");
    }

    @Test
    void asksForTheRequestAgainWhenTheExceptionCarriesNoRecord() throws Exception {
        String body = bodyOf(mapper.onDuplicateRequest(new DuplicateRequestException("already processed")));

        assertThat(body)
                .contains("\"data\":null")
                .contains("\"code\":\"" + ResponseCodes.PLAYER_OPERATION_IN_PROGRESS.getCode() + "\"");
    }

    /** Unreachable today, but it must not answer a zero balance as success if it ever becomes reachable. */
    @Test
    void doesNotReportSuccessOnADuplicateBet() throws Exception {
        String body = bodyOf(mapper.onDuplicateBet(new DuplicateBetException()));

        assertThat(body)
                .contains("\"data\":null")
                .doesNotContain("\"balance_amount\":0");
    }
}
