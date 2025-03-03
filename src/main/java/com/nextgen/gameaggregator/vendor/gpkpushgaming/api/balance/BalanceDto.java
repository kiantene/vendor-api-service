package com.nextgen.gameaggregator.vendor.gpkpushgaming.api.balance;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.dto.ActionDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BalanceDto extends ActionDto {
    @NotBlank
    @JsonProperty("api_token")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String apiToken;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String user;

    @NotBlank
    @Size(min = 10, max = 10)
    @Pattern(regexp = "\\d+")
    private String timestamp;
}
