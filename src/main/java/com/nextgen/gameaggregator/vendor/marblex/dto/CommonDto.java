package com.nextgen.gameaggregator.vendor.marblex.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto {
    @NotBlank
    @JsonProperty("TraceID")
    @Size(max = 255)
    private String traceId;

    @NotBlank
    @JsonProperty("PlayerID")
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String playerId;

    @NotBlank
    @JsonProperty("Currency")
    @Size(max = 5)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String currency;
}
