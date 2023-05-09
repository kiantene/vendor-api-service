package com.nextgen.gameaggregator.operator.game.url;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import jakarta.validation.constraints.*;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameUrlDto {

    @NotBlank(message = "UUID format only")
    @Size(min = 36, max = 36, message = "UUID format only")
    @Pattern(regexp = ValidationUtils.UUID_REGEX, message = "UUID format only") // Only alphanumeric allowed
    private String traceId;


    @NotBlank(message = "min 3 and max 20 alphanumeric")
    @Size(min = 3, max = 20 , message = "min 3 and max 20 alphanumeric")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX, message = "min 3 and max 20 alphanumeric only") // Only alphanumeric allowed
    private String username;

    @NotBlank(message = "min 3 and max 50 alphanumeric")
    @Size(min = 3, max = 50, message = "min 3 and max 50 alphanumeric")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "min 3 and max 50 alphanumeric") // Only alphanumeric allowed
    private String gameCode;

    @NotBlank(message = "2 alphanumeric")
    @Size(min = 2, max = 2, message = " 2 alphanumeric only")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX, message = "2 alphanumeric only") // Only alphanumeric allowed
    private String language;

    @NotBlank(message = "min 2 and max 10 alphanumeric")
    @Size(min = 2,max = 10, message = "min 2 and max 10 alphanumeric")
    @Pattern(regexp = ValidationUtils.WEB_OR_H5, message = "Platform is not supported.")
    private String platform;

    @NotBlank( message = "min 3 and max 10  characters")
    @Size(min = 3, max = 10, message = "min 3 and max 10  characters")
    private String currency;

//    @NotBlank( message = "website URL required")
//    @Size(min = 3, max = 2048, message = "min 3 and max 2048  characters")
    private String lobbyUrl;

//    @NotBlank( message = "IPv4 or IPv6 required")
    //https://www.techdreams.org/microsoft/sql-server/maximum-length-of-ip-address-15-ipv4-39ipv6/5467-20110127
//    @Size(min = 15, max = 39, message = "min 15 and max 39  characters")
    private String ipAddress;
}
