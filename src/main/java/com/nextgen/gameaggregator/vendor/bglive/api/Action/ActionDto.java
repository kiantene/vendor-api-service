package com.nextgen.gameaggregator.vendor.bglive.api.Action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionDto {

    @NotBlank
    @JsonProperty("method")
    private String method;
}
