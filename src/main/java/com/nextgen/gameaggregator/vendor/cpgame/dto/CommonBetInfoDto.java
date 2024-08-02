package com.nextgen.gameaggregator.vendor.cpgame.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonBetInfoDto {

    @NotBlank
    @JsonProperty("bet_id")
    @Size(max = 255)
    private String betId;
}
