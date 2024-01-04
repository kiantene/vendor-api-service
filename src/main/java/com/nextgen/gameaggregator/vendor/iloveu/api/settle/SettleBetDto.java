package com.nextgen.gameaggregator.vendor.iloveu.api.settle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettleBetDto {

    @NotNull
    @Range(min = 0, max = 1000000000000000L)
    @Digits(integer = 18, fraction = 4)
    @JsonProperty("TotalBet")
    public BigDecimal totalBet;

    @JsonProperty("BetDetail")
    public String betDetail;

    @NotNull
    @Range(min = 0, max = 1000000000000000L)
    @Digits(integer = 18, fraction = 4)
    @JsonProperty("ValidBet")
    public BigDecimal validBet;
}
