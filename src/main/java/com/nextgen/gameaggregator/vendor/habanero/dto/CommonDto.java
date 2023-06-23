package com.nextgen.gameaggregator.vendor.habanero.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.facai.constant.ResponseCodes;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonDto {

    @NotBlank(message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @JsonProperty("AgentCode")
    private String agentCode;

    @NotBlank(message = ResponseCodes.CURRENCY_MISSING)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.CURRENCY_MISSING)
    @Size(min = 3, max = 4, message = ResponseCodes.CURRENCY_MISSING)
    @JsonProperty("Currency")
    private String currency;

    @NotBlank(message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @JsonProperty("Params")
    private String params;

    @NotBlank(message = ResponseCodes.PARAM_CONTAIN_ERROR)
    @JsonProperty("Sign")
    private String sign;
}
