package com.nextgen.gameaggregator.vendor.hacksaw.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.hacksaw.api.action.ActionDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceBetSettleRefundDto extends ActionDto {
    @NotBlank
    @Size(min = 1, max = 64)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String externalPlayerId;

    @NotBlank
    @Size(min = 1, max = 64)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String externalSessionId;

}
