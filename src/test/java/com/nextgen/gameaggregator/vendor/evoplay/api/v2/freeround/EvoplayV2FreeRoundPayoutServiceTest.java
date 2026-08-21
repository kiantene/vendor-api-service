package com.nextgen.gameaggregator.vendor.evoplay.api.v2.freeround;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.vendor.evoplay.api.freeround.EvoplayBalanceIncreaseValidator;
import com.nextgen.gameaggregator.vendor.evoplay.api.freeround.EvoplayFreeRoundPayoutRequestFactory;
import com.nextgen.gameaggregator.vendor.evoplay.api.freeround.EvoplayFreeRoundPayoutService;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.DataDto;
import com.nextgen.gameaggregator.vendor.evoplay.constant.ResponseCodes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EvoplayV2FreeRoundPayoutServiceTest {

    @Test
    void should_fail_negative_balance_increase_amount_without_processing_payout() {
        EvoplayFreeRoundPayoutService payoutService = mock(EvoplayFreeRoundPayoutService.class);
        EvoplayV2FreeRoundPayoutService service = new EvoplayV2FreeRoundPayoutService(
                new EvoplayBalanceIncreaseValidator(),
                new EvoplayFreeRoundPayoutRequestFactory(),
                payoutService,
                new EvoplayV2FreeRoundResponseAdapter(),
                mock(GameSessionService.class)
        );

        var response = service.payout(callback("-0.01"), httpRequestLog());

        assertThat(response.getStatus()).isEqualTo(ResponseCodes.INVALID_REQUEST_ERROR.status);
        assertThat(response.getError().getMessage()).isEqualTo(ResponseCodes.INVALID_REQUEST_ERROR.message);
        verify(payoutService, never()).payout(any(), any());
    }

    @Test
    void should_fail_promo_win_when_currency_does_not_match_session() {
        EvoplayFreeRoundPayoutService payoutService = mock(EvoplayFreeRoundPayoutService.class);
        GameSessionService gameSessionService = mock(GameSessionService.class);
        when(gameSessionService.getLastGameSessionByVendorPlayerUsername("vendor-player-1"))
                .thenReturn(gameSession("USD"));
        EvoplayV2FreeRoundPayoutService service = new EvoplayV2FreeRoundPayoutService(
                new EvoplayBalanceIncreaseValidator(),
                new EvoplayFreeRoundPayoutRequestFactory(),
                payoutService,
                new EvoplayV2FreeRoundResponseAdapter(),
                gameSessionService
        );

        var response = service.payoutWin(winCallback("RANDOM", "10.00"), httpRequestLog());

        assertThat(response.getStatus()).isEqualTo(ResponseCodes.INVALID_REQUEST_ERROR.status);
        assertThat(response.getError().getMessage()).isEqualTo(ResponseCodes.INVALID_REQUEST_ERROR.message);
        verify(payoutService, never()).payout(any(), any());
    }

    @Test
    void should_fail_promo_win_when_amount_is_null() {
        EvoplayFreeRoundPayoutService payoutService = mock(EvoplayFreeRoundPayoutService.class);
        GameSessionService gameSessionService = mock(GameSessionService.class);
        when(gameSessionService.getLastGameSessionByVendorPlayerUsername("vendor-player-1"))
                .thenReturn(gameSession("USD"));
        EvoplayV2FreeRoundPayoutService service = new EvoplayV2FreeRoundPayoutService(
                new EvoplayBalanceIncreaseValidator(),
                new EvoplayFreeRoundPayoutRequestFactory(),
                payoutService,
                new EvoplayV2FreeRoundResponseAdapter(),
                gameSessionService
        );

        var response = service.payoutWin(winCallback("USD", null), httpRequestLog());

        assertThat(response.getStatus()).isEqualTo(ResponseCodes.INVALID_REQUEST_ERROR.status);
        assertThat(response.getError().getMessage()).isEqualTo(ResponseCodes.INVALID_REQUEST_ERROR.message);
        verify(payoutService, never()).payout(any(), any());
    }

    @Test
    void should_fail_promo_win_when_currency_is_blank() {
        EvoplayFreeRoundPayoutService payoutService = mock(EvoplayFreeRoundPayoutService.class);
        GameSessionService gameSessionService = mock(GameSessionService.class);
        when(gameSessionService.getLastGameSessionByVendorPlayerUsername("vendor-player-1"))
                .thenReturn(gameSession("USD"));
        EvoplayV2FreeRoundPayoutService service = new EvoplayV2FreeRoundPayoutService(
                new EvoplayBalanceIncreaseValidator(),
                new EvoplayFreeRoundPayoutRequestFactory(),
                payoutService,
                new EvoplayV2FreeRoundResponseAdapter(),
                gameSessionService
        );

        var response = service.payoutWin(winCallback(" ", "10.00"), httpRequestLog());

        assertThat(response.getStatus()).isEqualTo(ResponseCodes.INVALID_REQUEST_ERROR.status);
        assertThat(response.getError().getMessage()).isEqualTo(ResponseCodes.INVALID_REQUEST_ERROR.message);
        verify(payoutService, never()).payout(any(), any());
    }

    private CallbackDto callback(String amount) {
        DataDto data = new DataDto();
        data.setId("txn-1");
        data.setUser_id("vendor-player-1");
        data.setType("free_rounds_win");
        data.setEvent_id("registry-1");
        data.setCurrency("USD");
        data.setAmount(amount);

        CallbackDto callback = new CallbackDto();
        callback.setName("BalanceIncrease");
        callback.setData(data);
        return callback;
    }

    private CallbackDto winCallback(String currency, String amount) {
        DataDto data = new DataDto();
        data.setId("txn-1");
        data.setAction_id("action-1");
        data.setRound_id("round-1");
        data.setCurrency(currency);
        data.setAmount(amount);

        com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.DetailsDto details =
                new com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.DetailsDto();
        details.setPayout("92");
        details.setExtrabonus_registration_id("registration-1");
        data.setDetailsDto(details);

        CallbackDto callback = new CallbackDto();
        callback.setName("win");
        callback.setUsername("vendor-player-1");
        callback.setCallback_id("callback-1");
        callback.setData(data);
        return callback;
    }

    private GameSession gameSession(String currency) {
        GameSession gameSession = new GameSession();
        gameSession.setVendorCurrencyCode(currency);
        return gameSession;
    }

    private HttpRequestLog httpRequestLog() {
        HttpRequestLog log = new HttpRequestLog();
        log.setId("trace-1");
        return log;
    }
}
