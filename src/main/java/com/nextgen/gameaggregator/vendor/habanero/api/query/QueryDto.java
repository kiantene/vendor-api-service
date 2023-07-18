package com.nextgen.gameaggregator.vendor.habanero.api.query;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.habanero.dto.BaseGameDto;
import com.nextgen.gameaggregator.vendor.habanero.dto.SubAuthDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryDto {

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @JsonProperty("type")
    public String type;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9:._-]+$")
    @JsonProperty("dtsent")
    public String dtSent;

    @JsonProperty("basegame")
    public BaseGameDto baseGame;

    @JsonProperty("auth")
    public SubAuthDto subAuth;

    @JsonProperty("queryrequest")
    public QueryRequestDto queryRequestDto;
}
