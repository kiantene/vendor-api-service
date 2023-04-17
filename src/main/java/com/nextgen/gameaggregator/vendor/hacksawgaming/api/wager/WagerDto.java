package com.nextgen.gameaggregator.vendor.hacksawgaming.api.wager;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class WagerDto {

    @NotBlank
    @Size(max =7)
    public String brandId;

    @NotBlank
    @Size(min = 32)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    public String sign;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(max = 32)
    public String token;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(min = 3, max = 20)
    public String brandUid;

    @NotBlank
    @Size(min = 3, max =4)
    @Pattern(regexp = "[a-zA-Z]+")
    public String currency;

    @NotBlank
    @Digits(integer = 16, fraction = 2)
    public BigDecimal amount;

    @NotBlank
    @Digits(integer = 16, fraction = 6)
    public BigDecimal jackpot_contribution;

    @NotBlank
    @Digits(integer = Integer.MAX_VALUE, fraction = 0)
    public Integer gameId;

    @NotBlank
    @Size(max = 50)
    public String gameName;

    @NotBlank
    @Size(max = 64)
    public String roundId;

    @NotBlank
    @Size(max = 64)
    public String wagerId;

    @NotBlank
    @Size(max = 20)
    public String provider;

    @NotBlank
    @Pattern(regexp = "[12]")
    // 1=Normal; 2=Tip
    public Integer betType;

    @NotBlank
    @Pattern(regexp = "[01]")
    // 0= Unfinished, 1= Round Finish
    public Integer isEndround;
}
