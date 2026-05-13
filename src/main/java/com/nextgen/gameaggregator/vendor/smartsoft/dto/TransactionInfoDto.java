package com.nextgen.gameaggregator.vendor.smartsoft.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nextgen.gameaggregator.vendor.smartsoft.service.DateDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

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

    @JsonProperty("BetTransactionId")
    private String betTransactionId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("GameName")
    private String gameName;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("GameNumber")
    private String gameNumber;

    @NotNull
    @JsonProperty("CashierTransactionId")
    private int cashierTransactionId;
    
    @JsonProperty("TransactionDate")
    @JsonDeserialize(using = DateDeserializer.class)
    private OffsetDateTime transactionDate;
}
