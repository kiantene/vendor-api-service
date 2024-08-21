package com.nextgen.gameaggregator.vendor.cpgame.api.canceldebit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.service.HttpService;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelBetDto implements RollbackData {

    @NotBlank
    private String appid;
    @NotNull
    @PositiveOrZero(message = "negative number")
    @Digits(integer = 10, fraction = 0)
    private Long time;
    @NotBlank
    @Size(max = 1000)
    private String token;
    @NotBlank
    private String message;
    @NotNull
    private MessageDto messageDto;

    public void convertStringToJsonObject(String message) throws JsonProcessingException {
        MessageDto subDto = HttpService.convertJsonToDto(message, MessageDto.class);

        setMessageDto(subDto);
    }

    @Override
    public String getRollbackId() {
        return messageDto.getBetId();
    }

    @Override
    public Long getVendorSettledTime() {
        return this.time * 1000;
    }

    @Override
    public String getRoundId() {
        return null;
    }
}
