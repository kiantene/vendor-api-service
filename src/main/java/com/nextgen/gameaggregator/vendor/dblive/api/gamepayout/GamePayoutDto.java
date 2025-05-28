package com.nextgen.gameaggregator.vendor.dblive.api.gamepayout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.dblive.dto.CommonDto;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GamePayoutDto extends CommonDto {
    @NotNull
    @Digits(integer = 19, fraction = 0)
    private BigDecimal transferNo;
}
