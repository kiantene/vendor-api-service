package com.nextgen.gameaggregator.vendor.habanero.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubAuthDto {

    @JsonProperty("username")
    public String username;

    @NotBlank
    @JsonProperty("passkey")
    public String passkey;

    @JsonProperty("machinename")
    public String machinenName;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("locale")
    public String locale;

    @NotBlank
    @JsonProperty("brandid")
    public String brandid;
}
