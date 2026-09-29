package com.nextgen.gameaggregator.vendor.pgsoft.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.CashTransferInOutDto;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.CashTransferInOutVo;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.PromoRequestMapper;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.PromoResponseMapper;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.pgsoft.vo.ResponseVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * A duplicate FreeGameToCash payout is answered here rather than in PGSoftExceptionMapper, because the
 * response PG SOFT expects carries fields only the request holds: real_transfer_amount, which they
 * validate against what they sent, and updated_time.
 *
 * <p>Asserted on the serialised body — the original defect (ONEAPI-535) was a required field being
 * dropped on the way out by NON_NULL, which an assertion on the object would not have caught.
 */
@ExtendWith(MockitoExtension.class)
class PGSoftPromoPayoutServiceTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock
    private PromoPayoutService promoPayoutService;

    private PGSoftPromoPayoutService service;

    @BeforeEach
    void setUp() {
        service = new PGSoftPromoPayoutService(
                new PromoRequestMapper(), new PromoResponseMapper(), promoPayoutService);
    }

    /** Mirrors the live FreeGameToCash request from the ONEAPI-535 logs. */
    private static CashTransferInOutDto request() {
        CashTransferInOutDto dto = new CashTransferInOutDto();
        dto.setTransactionId("2087259923267453955-2087259923267453955-403-0");
        dto.setPlayerName("37b8xvtl07g");
        dto.setCurrencyCode("BRL");
        dto.setFreeGameId(267331027);
        dto.setTransferAmount(new BigDecimal("5.36"));
        dto.setRealTransferAmount(new BigDecimal("5.36"));
        dto.setUpdatedTime(1786476519774L);
        return dto;
    }

    private String payout(CashTransferInOutDto dto) throws Exception {
        ResponseEntity<ResponseVo<CashTransferInOutVo>> response =
                service.doPromoPayout(dto, new HttpRequestLog());
        return JSON.writeValueAsString(response.getBody());
    }

    private void duplicateOf(RequestIdempotentLog record) {
        when(promoPayoutService.initialise(any())).thenReturn(promoPayoutService);
        when(promoPayoutService.process(any()))
                .thenThrow(new DuplicateRequestException("already processed", record));
    }

    @Test
    void recognisesFreeGameToCashAsAPromoPayout() {
        assertThat(service.isPromoPayout(request())).isTrue();
    }

    /**
     * PG SOFT asks for the previous successful response when a transaction_id is resent, so the stored
     * balance and currency come back alongside the request's own amounts.
     */
    @Test
    void repeatsTheStoredPayoutOnADuplicateOfACompletedRequest() throws Exception {
        RequestIdempotentLog completed = new RequestIdempotentLog();
        completed.setCurrency("BRL");
        completed.setBalance(new BigDecimal("128.44"));
        duplicateOf(completed);

        assertThat(payout(request()))
                .contains("\"currency_code\":\"BRL\"")
                .contains("\"balance_amount\":128.44")
                .contains("\"real_transfer_amount\":5.36")
                .contains("\"updated_time\":1786476519774")
                .contains("\"error\":null");
    }

    /**
     * The currency is recorded only once a payout lands, so without one the original has not completed.
     * Answering success with the zero the record is created with is what told PG SOFT a payout had
     * happened when it had not — the ONEAPI-535 incident.
     */
    @Test
    void asksForTheRequestAgainWhenTheOriginalHasNotCompleted() throws Exception {
        RequestIdempotentLog unfinished = new RequestIdempotentLog();
        unfinished.setBalance(BigDecimal.ZERO);
        duplicateOf(unfinished);

        assertThat(payout(request()))
                .contains("\"data\":null")
                .contains("\"code\":\"" + ResponseCodes.PLAYER_OPERATION_IN_PROGRESS.getCode() + "\"")
                .doesNotContain("\"balance_amount\":0");
    }

    @Test
    void answersAFirstTimePayoutFromTheOperatorBalance() throws Exception {
        when(promoPayoutService.initialise(any())).thenReturn(promoPayoutService);
        when(promoPayoutService.process(any()))
                .thenAnswer(invocation -> {
                    PromoPayoutContext context = invocation.getArgument(0);
                    return new PlayerBalanceData(
                            context.getVendorPlayerUsername(), "BRL", new BigDecimal("128.44"), 1L);
                });

        assertThat(payout(request()))
                .contains("\"currency_code\":\"BRL\"")
                .contains("\"balance_amount\":128.44")
                .contains("\"real_transfer_amount\":5.36")
                .contains("\"error\":null");
    }
}
