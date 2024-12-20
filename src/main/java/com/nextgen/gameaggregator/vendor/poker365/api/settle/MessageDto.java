package com.nextgen.gameaggregator.vendor.poker365.api.settle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDto {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("status")
    private String status;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("count")
    private String count;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("transactions")
    private List<TransactionsDto> transactionsDto;


}
