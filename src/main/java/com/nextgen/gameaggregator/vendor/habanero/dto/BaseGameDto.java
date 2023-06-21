package com.nextgen.gameaggregator.vendor.habanero.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseGameDto {

    @NotBlank
    @JsonProperty("brandgameid")
    public String brandGameId;

    @NotBlank
    @JsonProperty("keyname")
    public String keyName;
}
