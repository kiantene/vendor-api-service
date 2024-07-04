package com.nextgen.gameaggregator.vendor.cpgame.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {
    @Autowired
    private HttpService httpService;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String appid;

    @NotNull
    @Pattern(regexp = "\\d{10}")
    private String time;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String token;

    @NotBlank
    @Pattern(regexp = "^[^\\u4E00-\\u9FFF]*$")
    private String message;

    @NotNull
    private MessageDto messageDto;

    public void convertStringToJsonObject(String message) throws JsonProcessingException {
        MessageDto subDto = HttpService.convertJsonToDto(message, MessageDto.class);

        setMessageDto(subDto);
    }
}
