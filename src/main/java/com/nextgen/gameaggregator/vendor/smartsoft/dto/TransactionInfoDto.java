package com.nextgen.gameaggregator.vendor.smartsoft.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionInfoDto {

    @JsonProperty("Source")
    private String source;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("RoundId")
    private String roundId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("GameName")
    private String gameName;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("GameNumber")
    private String gameNumber;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("CashierTransactionId")
    private int cashierTransactionId;
}
