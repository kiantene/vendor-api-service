package com.nextgen.gameaggregator.vendor.evoplay.api.freeround;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.vendor.evoplay.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.dto.DataDto;
import com.nextgen.gameaggregator.vendor.evoplay.dto.DetailsDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvoplayFreeRoundPayoutRequestFactoryTest {
    private final EvoplayFreeRoundPayoutRequestFactory factory = new EvoplayFreeRoundPayoutRequestFactory();

    @Test
    void fromV1Win_mapsPromoPayoutFieldsFromWinCallback() {
        CallbackDto callbackDto = new CallbackDto();
        callbackDto.setCallback_id("callback-1");

        DataDto data = new DataDto();
        data.setAction_id("action-1");
        data.setCurrency("BRL");
        data.setAmount("25");

        DetailsDto details = new DetailsDto();
        details.setPayout("92");
        details.setTotal_win_for_action_in_money("35");
        details.setTotal_win("35");
        details.setExtrabonus_registration_id("registration-1");
        data.setDetailsDto(details);
        callbackDto.setData(data);

        GameSession gameSession = new GameSession();
        gameSession.setVendorPlayerUsername("vendor-player-1");

        EvoplayFreeRoundPayoutRequest request = factory.fromV1Win(callbackDto, gameSession);

        assertThat(request.getId()).isEqualTo("callback-1");
        assertThat(request.getUserId()).isEqualTo("vendor-player-1");
        assertThat(request.getEventId()).isEqualTo("registration-1");
        assertThat(request.getCurrency()).isEqualTo("BRL");
        assertThat(request.getAmount()).isEqualByComparingTo("25");
        assertThat(request.isPlayerUuidCampaignLookup()).isTrue();
    }

    @Test
    void fromV1Win_fallsBackToActionIdWhenCallbackIdIsBlank() {
        CallbackDto callbackDto = new CallbackDto();
        callbackDto.setCallback_id(" ");

        DataDto data = new DataDto();
        data.setAction_id("action-1");
        data.setCurrency("BRL");
        data.setAmount("7");

        DetailsDto details = new DetailsDto();
        details.setPayout("92");
        details.setTotal_win_for_action_in_money("35");
        details.setExtrabonus_registration_id("registration-1");
        data.setDetailsDto(details);
        callbackDto.setData(data);

        GameSession gameSession = new GameSession();
        gameSession.setVendorPlayerUsername("vendor-player-1");

        EvoplayFreeRoundPayoutRequest request = factory.fromV1Win(callbackDto, gameSession);

        assertThat(request.getId()).isEqualTo("action-1");
    }

    @Test
    void fromV1Win_usesDataAmountWhenDetailWinFieldsAreZero() {
        CallbackDto callbackDto = new CallbackDto();
        callbackDto.setCallback_id("callback-1");

        DataDto data = new DataDto();
        data.setAction_id("action-1");
        data.setCurrency("BRL");
        data.setAmount("7.8");

        DetailsDto details = new DetailsDto();
        details.setPayout("92");
        details.setTotal_win("7.8");
        details.setTotal_win_for_action_in_money("0");
        details.setExtrabonus_registration_id("registration-1");
        data.setDetailsDto(details);
        callbackDto.setData(data);

        GameSession gameSession = new GameSession();
        gameSession.setVendorPlayerUsername("vendor-player-1");

        EvoplayFreeRoundPayoutRequest request = factory.fromV1Win(callbackDto, gameSession);

        assertThat(request.getAmount()).isEqualByComparingTo("7.8");
    }

    @Test
    void fromV2Win_usesDataAmountWhenDetailWinFieldsAreZero() {
        com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.CallbackDto callbackDto =
                new com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.CallbackDto();
        callbackDto.setCallback_id("callback-1");
        callbackDto.setUsername("vendor-player-1");

        com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.DataDto data =
                new com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.DataDto();
        data.setAction_id("action-1");
        data.setCurrency("BRL");
        data.setAmount("7.8");

        com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.DetailsDto details =
                new com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.DetailsDto();
        details.setPayout("92");
        details.setTotal_win("7.8");
        details.setTotal_win_for_action_in_money("0");
        details.setExtrabonus_registration_id("registration-1");
        data.setDetailsDto(details);
        callbackDto.setData(data);

        EvoplayFreeRoundPayoutRequest request = factory.fromV2Win(callbackDto);

        assertThat(request.getAmount()).isEqualByComparingTo("7.8");
    }

    @Test
    void fromV1Win_rejectsNullAmount() {
        CallbackDto callbackDto = new CallbackDto();
        callbackDto.setCallback_id("callback-1");

        DataDto data = new DataDto();
        data.setAction_id("action-1");
        data.setCurrency("BRL");

        DetailsDto details = new DetailsDto();
        details.setPayout("92");
        details.setExtrabonus_registration_id("registration-1");
        data.setDetailsDto(details);
        callbackDto.setData(data);

        GameSession gameSession = new GameSession();
        gameSession.setVendorPlayerUsername("vendor-player-1");

        assertThatThrownBy(() -> factory.fromV1Win(callbackDto, gameSession))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data.amount");
    }

    @Test
    void fromV1Win_rejectsNegativeAmount() {
        CallbackDto callbackDto = new CallbackDto();
        callbackDto.setCallback_id("callback-1");

        DataDto data = new DataDto();
        data.setAction_id("action-1");
        data.setCurrency("BRL");
        data.setAmount("-0.01");

        DetailsDto details = new DetailsDto();
        details.setPayout("92");
        details.setExtrabonus_registration_id("registration-1");
        data.setDetailsDto(details);
        callbackDto.setData(data);

        GameSession gameSession = new GameSession();
        gameSession.setVendorPlayerUsername("vendor-player-1");

        assertThatThrownBy(() -> factory.fromV1Win(callbackDto, gameSession))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive or zero");
    }
}
