package com.nextgen.gameaggregator.vendor.hacksawgaming.api.balance;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BalanceDto {

    @NotBlank
    @Size(max =7)
    public String brandId;

    @NotBlank
    @Size(min = 32)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    public String sign;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(min = 3, max = 20)
    public String brandUid;

    @NotBlank
    @Size(min = 3, max =4)
    @Pattern(regexp = "[a-zA-Z]+")
    public String currency;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(max = 32)
    public String token;

}
