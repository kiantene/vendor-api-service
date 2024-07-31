package com.nextgen.gameaggregator.vendor.cpgame.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.cpgame.dto.CommonDto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollBackDto extends CommonDto implements RollbackData {

    @NotNull
    private MessageDto messageDto;

    public void convertStringToJsonObject(String message) throws JsonProcessingException {
        MessageDto subDto = HttpService.convertJsonToDto(message, MessageDto.class);

        setMessageDto(subDto);
    }

    @Override
    public String getRollbackId() {
        return this.messageDto.getBetId();
    }

    @Override
    public Long getVendorSettledTime() {
        return this.getTime() * 1000;
    }
}
