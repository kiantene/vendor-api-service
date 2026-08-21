package com.nextgen.gameaggregator.vendor.evoplay.api.freeround;

import com.nextgen.gameaggregator.vendor.evoplay.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.dto.DataDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvoplayBalanceIncreaseValidatorTest {
    private final EvoplayBalanceIncreaseValidator validator = new EvoplayBalanceIncreaseValidator();

    @Test
    void should_validate_free_round_win_without_token_or_signature() {
        CallbackDto request = validV1("free_rounds_win");
        request.setToken(null);
        request.setSignature(null);

        assertThatCode(() -> validator.validateV1(request)).doesNotThrowAnyException();
    }

    @Test
    void should_require_event_id_for_free_round_win() {
        CallbackDto request = validV1("free_rounds_win");
        request.getData().setEvent_id(null);

        assertThatThrownBy(() -> validator.validateV1(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data.event_id");
    }

    @Test
    void should_reject_non_free_round_type_when_validator_is_called() {
        CallbackDto request = validV1("gift");

        assertThatThrownBy(() -> validator.validateV1(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("free_rounds_win");
    }

    @Test
    void should_leave_non_free_round_decision_to_controller() {
        assertThat(validator.isFreeRoundWin("BalanceIncrease", "gift")).isFalse();
    }

    @Test
    void should_reject_negative_amount() {
        CallbackDto request = validV1("free_rounds_win");
        request.getData().setAmount("-0.01");

        assertThatThrownBy(() -> validator.validateV1(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive or zero");
    }

    @Test
    void should_detect_free_round_win_case_insensitively() {
        assertThat(validator.isFreeRoundWin("BalanceIncrease", "FREE_ROUNDS_WIN")).isTrue();
        assertThat(validator.isFreeRoundWin("BalanceIncrease", "gift")).isFalse();
        assertThat(validator.isFreeRoundWin("win", "free_rounds_win")).isFalse();
    }

    private CallbackDto validV1(String type) {
        DataDto data = new DataDto();
        data.setId("txn-1");
        data.setUser_id("vendor-player-1");
        data.setType(type);
        data.setEvent_id("registry-1");
        data.setCurrency("USD");
        data.setAmount("10.50");

        CallbackDto request = new CallbackDto();
        request.setName("BalanceIncrease");
        request.setData(data);
        return request;
    }
}
