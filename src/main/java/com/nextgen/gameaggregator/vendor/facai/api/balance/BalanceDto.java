package com.nextgen.gameaggregator.vendor.facai.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.facai.constant.ResponseCodes;
import lombok.Data;

import jakarta.validation.constraints.*;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {
    @NotBlank(message = ResponseCodes.PLAYER_NOT_FOUND)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.PLAYER_NOT_FOUND)
    @Size(min = 2, max = 30, message = ResponseCodes.PLAYER_NOT_FOUND)
    public String MemberAccount;

    @NotBlank(message = ResponseCodes.CURRENCY_MISSING)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.CURRENCY_MISSING)
    @Size(min = 3, max = 4, message = ResponseCodes.CURRENCY_MISSING)
    public String Currency;

    @PositiveOrZero(message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @NotNull(message = ResponseCodes.PARAM_CONTAIN_ERROR)
    public Integer GameID;

    @NotNull(message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @Digits(integer = 13, fraction = 0, message = ResponseCodes.PARAM_CONTAIN_ERROR)
    public Long Ts;
}
