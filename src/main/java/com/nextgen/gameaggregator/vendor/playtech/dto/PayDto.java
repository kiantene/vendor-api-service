package com.nextgen.gameaggregator.vendor.playtech.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayDto {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("transactionCode")
    private String transactionCode;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("transactionDate")
    private String transactionDate;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @DecimalMin(value = "0.0")
    @JsonProperty("amount")
    private BigDecimal amount;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("type")
    private String type;

    @JsonProperty("relatedTransactionCode")
    private String relatedTransactionCode;

}
