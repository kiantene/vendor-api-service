package com.nextgen.gameaggregator.vendor.cpgame.api.credit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.cpgame.dto.CommonBetInfoDto;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetInfoDto extends CommonBetInfoDto {

    @NotNull
    @JsonProperty("win_amount")
    @Digits(integer = 20, fraction = 8)
    private BigDecimal winAmount;

}
