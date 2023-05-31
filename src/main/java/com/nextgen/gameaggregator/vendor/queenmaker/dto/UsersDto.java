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

    @NotBlank(message = "authtoken Regex fail 1")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "authtoken invalid regex")
    @Size(min = 1, max = 2000, message = "authtoken out of range")
    private String authtoken;

    @NotBlank(message = "userid empty")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "userid invalid regex")
    @Size(min = 1, max = 50, message = "userid out of range")
    private String userid;

    @NotBlank(message = "brandcode empty")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "brandcode invalid regex")
    @Size(min = 1, max = 20, message = "brandcode out of range")
    private String brandcode;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "lang invalid regex")
    @Size(min = 1, max = 5, message = "lang out of range")
    private String lang; // Optional

    @NotBlank(message = "cur empty")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "cur invalid regex")
    @Size(min = 3, max = 8, message = "cur out of range")
    private String cur;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "walletcode invalid regex")
    @Size(min = 1, max = 20, message = "walletcode out of range")
    private String walletcode; // Optional
}
