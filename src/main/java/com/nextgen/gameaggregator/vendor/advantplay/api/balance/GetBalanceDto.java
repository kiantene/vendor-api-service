package com.nextgen.gameaggregator.vendor.advantplay.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class GetBalanceDto {

    @NotBlank
    @Size(min = 1, max = 50)
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}[+-]\\d{2}:\\d{2}")
    private String timestamp;

    @NotBlank
    @Pattern(regexp = ValidationUtils.UUID_REGEX)
    @Size(min = 1, max = 50)
    private String seq;

    private String brandCode;

    @NotBlank
    @Size(min = 1, max = 20)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String siteCode;
    
    @JsonProperty("OPToken")
    @NotBlank
    @Size(min = 1, max = 100)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String opToken;

    @Size(min = 1, max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String playerId;
}
