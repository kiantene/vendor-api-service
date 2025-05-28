package com.nextgen.gameaggregator.vendor.dblive.api.activitypayout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActivityPayoutCommonDto {

    @NotNull
    @Digits(integer = 15, fraction = 5)
    private BigDecimal payoutAmount;
    @NotNull
    @Digits(integer = 13, fraction = 0)
    private Long payoutTime;
    @NotBlank
    @Size(max = 10)
    private String payoutType;
    @NotNull
    @Digits(integer = 19, fraction = 0)
    private BigDecimal transferNo;
    @NotBlank
    @Size(max = 50)
    private String loginName;
    @NotBlank
    @Size(max = 3)
    private String currency;
}
