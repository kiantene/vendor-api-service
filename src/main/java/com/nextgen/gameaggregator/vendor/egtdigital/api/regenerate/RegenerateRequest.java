package com.nextgen.gameaggregator.vendor.egtdigital.api.regenerate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.egtdigital.dto.RequestCommonDto;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public class RegenerateRequest extends RequestCommonDto {

    @NotBlank
    @JsonProperty("gameKey")
    private String gameKey;
}
