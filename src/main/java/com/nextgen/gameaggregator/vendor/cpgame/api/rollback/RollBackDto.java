package com.nextgen.gameaggregator.vendor.cpgame.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.service.HttpService;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollBackDto implements RollbackData {

    private String appid;
    private Long time;
    private String token;
    private String message;
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
}
