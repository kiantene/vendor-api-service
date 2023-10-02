package com.nextgen.gameaggregator.vendor.dotconnections.api.balance;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.dotconnections.dto.CommonDto;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BalanceDto extends CommonDto {

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX, message = ResponseCodes.PLAYER_NOT_EXIST)
    @Size(min = 32, max = 32, message = ResponseCodes.PLAYER_NOT_EXIST)
    public String token;

}
