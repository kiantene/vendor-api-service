package com.nextgen.gameaggregator.vendor.poker365.api.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDto {

    @Size(max = 255)
    @JsonProperty("action")
    private String action;

//    @Size(max = 255)
//    @JsonProperty("transactions")
//    private List<TransactionMessageDto> transactionMessageDto;
}
