package com.nextgen.gameaggregator.vendor.evoplay.api.freeround;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.vendor.evoplay.dto.DetailsDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EvoplayFreeRoundPayoutRequestFactory {

    public EvoplayFreeRoundPayoutRequest fromV1(com.nextgen.gameaggregator.vendor.evoplay.dto.CallbackDto request) {
        var data = request.getData();
        return EvoplayFreeRoundPayoutRequest.builder()
                .id(data.getId())
                .userId(data.getUser_id())
                .eventId(data.getEvent_id())
                .currency(data.getCurrency())
                .amount(parseAmount(data.getAmount()))
                .build();
    }

    public EvoplayFreeRoundPayoutRequest fromV1Win(com.nextgen.gameaggregator.vendor.evoplay.dto.CallbackDto request,
                                                   GameSession gameSession) {
        var data = request.getData();
        DetailsDto details = data.getDetailsDto();

        return EvoplayFreeRoundPayoutRequest.builder()
                .id(firstNotBlank(request.getCallback_id(), data.getAction_id(), data.getRound_id()))
                .userId(gameSession.getVendorPlayerUsername())
                .eventId(details.getExtrabonus_registration_id())
                .currency(data.getCurrency())
                .amount(parseAmount(data.getAmount()))
                .playerUuidCampaignLookup(true)
                .build();
    }

    public EvoplayFreeRoundPayoutRequest fromV2(com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.CallbackDto request) {
        var data = request.getData();
        return EvoplayFreeRoundPayoutRequest.builder()
                .id(data.getId())
                .userId(data.getUser_id())
                .eventId(data.getEvent_id())
                .currency(data.getCurrency())
                .amount(parseAmount(data.getAmount()))
                .build();
    }

    public EvoplayFreeRoundPayoutRequest fromV2Win(com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.CallbackDto request) {
        var data = request.getData();
        var details = data.getDetailsDto();

        return EvoplayFreeRoundPayoutRequest.builder()
                .id(firstNotBlank(request.getCallback_id(), data.getAction_id(), data.getRound_id()))
                .userId(request.getUsername())
                .eventId(details.getExtrabonus_registration_id())
                .currency(data.getCurrency())
                .amount(parseAmount(data.getAmount()))
                .playerUuidCampaignLookup(true)
                .build();
    }

    private BigDecimal parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            throw new IllegalArgumentException("data.amount is required");
        }
        BigDecimal value = new BigDecimal(amount);
        if (value.signum() < 0) {
            throw new IllegalArgumentException("data.amount must be positive or zero");
        }
        return value;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
