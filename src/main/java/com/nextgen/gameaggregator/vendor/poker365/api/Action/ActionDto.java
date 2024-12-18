package com.nextgen.gameaggregator.vendor.poker365.api.Action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionDto {
    @NotBlank
    @JsonProperty("key")
    private String key;

    @NotBlank
    @JsonProperty("message")
    private String message;

}

