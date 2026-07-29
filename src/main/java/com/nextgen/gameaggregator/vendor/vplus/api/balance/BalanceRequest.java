package com.nextgen.gameaggregator.vendor.vplus.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceRequest {
    @NotNull
    private Long userId;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Username must contain only ASCII letters, digits, or underscores")
    private String username;

    @NotNull
    @JsonProperty("timestamp")
    private Long timeStamp;
}
