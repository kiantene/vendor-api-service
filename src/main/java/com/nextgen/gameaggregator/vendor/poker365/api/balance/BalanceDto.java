package com.nextgen.gameaggregator.vendor.poker365.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("key")
    private String key;

    @Valid
    @JsonProperty("message")
    private MessageDto message;

//    public void convertJsonToDto(String message) throws JsonProcessingException {
//        MessageDto messageDto = HttpService.convertJsonToDto(message, MessageDto.class);
//
//        setMessage(messageDto);
//    }
}
