package com.nextgen.gameaggregator.vendor.dblive.api.gamepayout;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GamePayoutDataVo {
    @NotNull
    private BigDecimal realAmount;
    @NotNull
    private BigDecimal balance;
    @NotBlank
    @Size(max = 255)
    private String loginName;
    @NotNull
    private BigDecimal badAmount;
}
