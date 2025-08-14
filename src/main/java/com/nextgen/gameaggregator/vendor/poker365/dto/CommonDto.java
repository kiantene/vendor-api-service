package com.nextgen.gameaggregator.vendor.poker365.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("key")
    private String key;

    @NotBlank
    @JsonProperty("message")
    private String message;
}
