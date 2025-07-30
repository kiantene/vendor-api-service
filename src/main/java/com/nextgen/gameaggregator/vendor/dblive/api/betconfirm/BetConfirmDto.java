package com.nextgen.gameaggregator.vendor.dblive.api.betconfirm;

import com.nextgen.gameaggregator.vendor.dblive.dto.CommonDto;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetConfirmDto extends CommonDto {
    @NotNull
    @Digits(integer = 19, fraction = 0)
    private BigDecimal transferNo;
}
