package com.nextgen.gameaggregator.vendor.spinix.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;
import org.apache.commons.collections.map.CaseInsensitiveMap;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BalanceDto {

    @NotBlank
    @Size(max =32)
    public String reqId;

    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    public String userId;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(max =50)
    public String userToken;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(max =24)
    public String gameId;

    @NotBlank
    @Size(min = 3, max =24)
    @Pattern(regexp = "[a-zA-Z]+")
    public String currency;

}
