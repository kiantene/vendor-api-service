package com.nextgen.gameaggregator.vendor.dotconnections.api.balance;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.ResponseCodes;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BalanceDto {

    @NotBlank
    @Size(max = 7)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    public String brandId;

    @NotBlank
    @Size(max = 32)
    @Pattern(regexp = "^[A-Z0-9]*$")
    public String sign;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX, message = ResponseCodes.PLAYER_NOT_EXIST)
    @Size(min = 32, max = 32, message = ResponseCodes.PLAYER_NOT_EXIST)
    public String token;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(min = 3, max = 20)
    public String brandUid;

    @NotBlank
    @Size(min = 3, max = 4)
    @Pattern(regexp = "[a-zA-Z]+")
    public String currency;

}
