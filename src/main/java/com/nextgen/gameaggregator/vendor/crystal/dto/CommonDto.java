package com.nextgen.gameaggregator.vendor.crystal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("currencyCode")
    private String currencyCode;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("playerId")
    private String playerId;

}
