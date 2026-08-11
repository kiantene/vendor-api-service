package com.nextgen.gameaggregator.vendor.evolutionv2.api.promo;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutConfig;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.vendor.evolution.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.evolutionv2.constant.EndPoints;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Evolution v2 promo-payout integration.
 */
@ExtendWith(MockitoExtension.class)
class PromoPayoutActionTest {

    @Mock private PromoPayoutRequestMapper requestMapper;
    @Mock private PromoPayoutResponseMapper responseMapper;
    @Mock private PromoPayoutService promoPayoutService;
    private PromoPayoutAction action;
    private PromoPayoutRequestDto request;
    private PromoPayoutContext context;

    @BeforeEach
    void setUp() {
        action = new PromoPayoutAction(requestMapper, responseMapper, promoPayoutService);

        PromoTransactionDto transaction = new PromoTransactionDto();
        transaction.setType("FreeRoundPlayableSpent");
        transaction.setId("promo-tx-123");
        transaction.setAmount(new BigDecimal("10.00"));

        request = new PromoPayoutRequestDto();
        request.setSid("sid-123");
        request.setUserId("player123");
        request.setCurrency("USD");
        request.setUuid("request-123");
        request.setPromoTransaction(transaction);

        context = PromoPayoutContext.builder()
                .vendorPlayerUsername("player123")
                .vendorCurrency("USD")
                .build();

        LogContext logContext = new LogContext();
        logContext.setVendorClassName(EndPoints.CLASS_NAME);
        LogContextHolder.set(logContext);
    }

    @AfterEach
    void tearDown() {
        LogContextHolder.clear();
    }

    @Test
    void promoPayout_delegatesToEngine() {
        PlayerBalanceData balanceData = new PlayerBalanceData(
                "player123",
                "USD",
                new BigDecimal("110.00"),
                1754373936436L
        );
        ResponseVo mappedResponse = new ResponseVo();
        mappedResponse.setBalance(balanceData.getBalance());

        when(requestMapper.toInternal(request)).thenReturn(context);
        when(promoPayoutService.initialise(context)).thenReturn(promoPayoutService);
        when(promoPayoutService.configure(any())).thenReturn(promoPayoutService);
        when(promoPayoutService.process(context)).thenReturn(balanceData);
        when(responseMapper.toVendor(context, balanceData)).thenReturn(mappedResponse);

        ResponseEntity<ResponseVo> result = action.promoPayout(request);

        assertThat(result.getBody()).isSameAs(mappedResponse);
        verify(promoPayoutService).process(context);
    }

    @Test
    void configure_enablesPlayerUuidCampaignLookupOnly() {
        PromoPayoutConfig config = new PromoPayoutConfig();

        action.configure(config, request);

        assertThat(config.isPlayerUuidCampaignLookup()).isTrue();
        assertThat(config.isBatch()).isFalse();
    }
}
