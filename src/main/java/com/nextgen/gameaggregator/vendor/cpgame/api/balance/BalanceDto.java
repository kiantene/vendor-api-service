package com.nextgen.gameaggregator.vendor.cpgame.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.cpgame.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.cpgame.dto.CommonMessageDto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto extends CommonDto {

    @NotNull
    private CommonMessageDto messageDto;

    public void convertStringToJsonObject(String message) throws JsonProcessingException {
        CommonMessageDto subDto = HttpService.convertJsonToDto(message, CommonMessageDto.class);

        setMessageDto(subDto);
    }

}
