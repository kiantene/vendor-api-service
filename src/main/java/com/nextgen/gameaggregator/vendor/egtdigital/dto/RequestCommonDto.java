package com.nextgen.gameaggregator.vendor.egtdigital.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestCommonDto {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("requestId")
    private String requestId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("timestamp")
    private String timestamp;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("playerId")
    private String playerId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("sessionId")
    private String sessionId;
}
