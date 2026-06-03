package com.nextgen.gameaggregator.vendor.spribe.api.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutConfig;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.vendor.spribe.api.v2.result.BetResultRequest;
import com.nextgen.gameaggregator.vendor.spribe.api.v2.result.FreebetDepositHandler;
import com.nextgen.gameaggregator.vendor.spribe.api.v2.result.FreebetPayoutContextMapper;
import com.nextgen.gameaggregator.vendor.spribe.api.v2.result.FreebetPayoutResponseMapper;
import com.nextgen.gameaggregator.vendor.spribe.response.SuccessResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreebetDepositHandlerTest {

    @Mock private PromoPayoutService promoPayoutService;
    @Mock private FreebetPayoutContextMapper contextMapper;
    @Mock private FreebetPayoutResponseMapper responseMapper;

    private FreebetDepositHandler handler;

    @BeforeEach
    void setUp() {
        handler = new FreebetDepositHandler(promoPayoutService, contextMapper, responseMapper);
    }

    private BetResultRequest buildFreebetRequest() {
        BetResultRequest req = new BetResultRequest();
        req.setUserId("bbz6waqpib");
        req.setCurrency("CNY");
        req.setAmount(new BigDecimal("2690"));
        req.setProvider("spribe_aviator");
        req.setProviderTxId("82467397");
        req.setGame("aviator");
        req.setAction("freebet");
        req.setSessionToken("2d9c87e11e7e4b629798d77c40757d30");
        req.setActionId("1777436");
        req.setOperatorFreeBetId("019e6c7b95c1779493052fbb22e59c94");
        req.setFreeBetTotalBetAmount(new BigDecimal("5000"));
        return req;
    }

    @Test
    @DisplayName("supports() returns true only for 'freebet'")
    void supports_freebetAction_returnsTrue() {
        assertThat(handler.supports("freebet")).isTrue();
        assertThat(handler.supports("bet")).isFalse();
        assertThat(handler.supports("promofreebet")).isFalse();
        assertThat(handler.supports("rainfreebet")).isFalse();
    }

    @Test
    @DisplayName("handle() calls configure with playerUuidCampaignLookup=true")
    void handle_configuresPlayerUuidCampaignLookup() {
        BetResultRequest request = buildFreebetRequest();
        PromoPayoutContext ctx = PromoPayoutContext.builder()
                .vendorPlayerUsername("bbz6waqpib")
                .vendorCurrency("CNY")
                .build();
        PlayerBalanceData balanceData = new PlayerBalanceData("bbz6waqpib", "CNY", new BigDecimal("15.32"), System.currentTimeMillis());
        SuccessResponse.Data data = SuccessResponse.Data.builder()
                .userId("bbz6waqpib").currency("CNY").newBalance(new BigDecimal("15320")).build();
        SuccessResponse expectedResponse = new SuccessResponse(data);

        when(contextMapper.toInternal(request)).thenReturn(ctx);
        when(promoPayoutService.initialise(ctx)).thenReturn(promoPayoutService);
        when(promoPayoutService.configure(any())).thenReturn(promoPayoutService);
        when(promoPayoutService.process(ctx)).thenReturn(balanceData);
        when(responseMapper.toVendor(ctx, balanceData)).thenReturn(expectedResponse);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<PromoPayoutConfig>> configCaptor =
                ArgumentCaptor.forClass(Consumer.class);

        handler.handle(request);

        verify(promoPayoutService).configure(configCaptor.capture());
        PromoPayoutConfig config = new PromoPayoutConfig();
        configCaptor.getValue().accept(config);
        assertThat(config.isPlayerUuidCampaignLookup()).isTrue();
    }

    @Test
    @DisplayName("handle() invokes initialise → configure → process in order")
    void handle_serviceChainOrder() {
        BetResultRequest request = buildFreebetRequest();
        PromoPayoutContext ctx = PromoPayoutContext.builder()
                .vendorPlayerUsername("bbz6waqpib")
                .vendorCurrency("CNY")
                .build();
        PlayerBalanceData balanceData = new PlayerBalanceData("bbz6waqpib", "CNY", new BigDecimal("15.32"), System.currentTimeMillis());
        SuccessResponse.Data data = SuccessResponse.Data.builder()
                .userId("bbz6waqpib").currency("CNY").newBalance(new BigDecimal("15320")).build();

        when(contextMapper.toInternal(request)).thenReturn(ctx);
        when(promoPayoutService.initialise(ctx)).thenReturn(promoPayoutService);
        when(promoPayoutService.configure(any())).thenReturn(promoPayoutService);
        when(promoPayoutService.process(ctx)).thenReturn(balanceData);
        when(responseMapper.toVendor(ctx, balanceData)).thenReturn(new SuccessResponse(data));

        handler.handle(request);

        var inOrder = inOrder(promoPayoutService);
        inOrder.verify(promoPayoutService).initialise(ctx);
        inOrder.verify(promoPayoutService).configure(any());
        inOrder.verify(promoPayoutService).process(ctx);
    }
}
