package com.nextgen.gameaggregator.vendor.bglive.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrdersDto {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("orderId")
    private String orderId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("gameId")
    private String gameId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("issueId")
    private String issueId;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @JacksonXmlProperty(localName = "amount")
    private BigDecimal amount;
}
