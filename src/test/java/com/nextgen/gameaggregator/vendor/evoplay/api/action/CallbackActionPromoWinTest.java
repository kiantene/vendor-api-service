package com.nextgen.gameaggregator.vendor.evoplay.api.action;

import com.nextgen.gameaggregator.service.UnsettledBetCachingService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.CurrencyNotSupportedException;
import com.nextgen.gameaggregator.vendor.evoplay.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.dto.DataDto;
import com.nextgen.gameaggregator.vendor.evoplay.dto.DetailsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CallbackActionPromoWinTest {
    @Mock
    private UnsettledBetCachingService unsettledBetCachingService;

    @InjectMocks
    private CallbackAction callbackAction;

    @Test
    void isPromoPayoutWin_returnsTrueForWinWithRegistrationIdAndPayout() throws Exception {
        boolean result = ReflectionTestUtils.invokeMethod(callbackAction, "isPromoPayoutWin", promoWin());

        assertThat(result).isTrue();
        verify(unsettledBetCachingService, never()).getTop1UnsettledBetWithRoundId("round-1");
    }

    @Test
    void isPromoPayoutWin_returnsFalseWhenRegistrationIdIsMissing() throws Exception {
        CallbackDto callbackDto = promoWin();
        callbackDto.getData().getDetailsDto().setExtrabonus_registration_id(null);

        boolean result = ReflectionTestUtils.invokeMethod(callbackAction, "isPromoPayoutWin", callbackDto);

        assertThat(result).isFalse();
        verify(unsettledBetCachingService, never()).getTop1UnsettledBetWithRoundId("round-1");
    }

    @Test
    void isPromoPayoutWin_returnsFalseWhenPayoutIsMissing() throws Exception {
        CallbackDto callbackDto = promoWin();
        callbackDto.getData().getDetailsDto().setPayout(null);

        boolean result = ReflectionTestUtils.invokeMethod(callbackAction, "isPromoPayoutWin", callbackDto);

        assertThat(result).isFalse();
        verify(unsettledBetCachingService, never()).getTop1UnsettledBetWithRoundId("round-1");
    }

    @Test
    void isPromoPayoutWin_doesNotCheckWinAmount() throws Exception {
        CallbackDto callbackDto = promoWin();
        callbackDto.getData().setAmount("92");

        boolean result = ReflectionTestUtils.invokeMethod(callbackAction, "isPromoPayoutWin", callbackDto);

        assertThat(result).isTrue();
        verify(unsettledBetCachingService, never()).getTop1UnsettledBetWithRoundId("round-1");
    }

    @Test
    void validatePromoPayoutWin_rejectsMismatchedCurrency() {
        CallbackDto callbackDto = promoWin();
        callbackDto.getData().setCurrency("RANDOM");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                callbackAction,
                "validatePromoPayoutWin",
                callbackDto,
                gameSession("USD")
        ))
                .isInstanceOf(java.lang.reflect.UndeclaredThrowableException.class)
                .hasCauseInstanceOf(CurrencyNotSupportedException.class);
    }

    @Test
    void validatePromoPayoutWin_rejectsNullAmount() {
        CallbackDto callbackDto = promoWin();
        callbackDto.getData().setCurrency("USD");
        callbackDto.getData().setAmount(null);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                callbackAction,
                "validatePromoPayoutWin",
                callbackDto,
                gameSession("USD")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data.amount");
    }

    @Test
    void validatePromoPayoutWin_rejectsBlankCurrency() {
        CallbackDto callbackDto = promoWin();
        callbackDto.getData().setCurrency(" ");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                callbackAction,
                "validatePromoPayoutWin",
                callbackDto,
                gameSession("USD")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data.currency");
    }

    private CallbackDto promoWin() {
        DetailsDto details = new DetailsDto();
        details.setPayout("92");
        details.setExtrabonus_registration_id("registration-1");

        DataDto data = new DataDto();
        data.setRound_id("round-1");
        data.setAmount("0");
        data.setCurrency("USD");
        data.setDetailsDto(details);

        CallbackDto callbackDto = new CallbackDto();
        callbackDto.setName("win");
        callbackDto.setData(data);
        return callbackDto;
    }

    private GameSession gameSession(String currency) {
        GameSession gameSession = new GameSession();
        gameSession.setVendorCurrencyCode(currency);
        return gameSession;
    }
}
