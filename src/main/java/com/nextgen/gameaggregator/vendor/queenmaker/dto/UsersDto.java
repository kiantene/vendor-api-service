package com.nextgen.gameaggregator.vendor.queenmaker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsersDto {

    @NotBlank(message = "authtoken cannot be empty")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Invalid authtoken Format")
    @Size(min = 1, max = 2000, message = "Invalid authtoken Size")
    private String authtoken;

    @NotBlank(message = "userid cannot be empty")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Invalid userid Format")
    @Size(min = 1, max = 50, message = "Invalid userid Size")
    private String userid;

    @NotBlank(message = "brandcode cannot be empty")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Invalid brandcode Format")
    @Size(min = 1, max = 20, message = "Invalid brandcode Size")
    private String brandcode;

    private String lang; // Optional

    @NotBlank(message = "cur cannot be empty")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Invalid cur Format")
    @Size(min = 3, max = 8, message = "Invalid cur Size")
    private String cur;

    private String walletcode; // Optional
}
