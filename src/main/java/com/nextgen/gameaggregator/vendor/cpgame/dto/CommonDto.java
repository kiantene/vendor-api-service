package com.nextgen.gameaggregator.vendor.cpgame.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.service.HttpService;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto {
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

    public void setMessageDto(String message) throws JsonProcessingException {
        this.messageDto = HttpService.convertJsonToDto(message, MessageDto.class);
    }
}
