package com.nextgen.gameaggregator.vendor.playtech.dto;

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
    @JsonProperty("requestId")
    private String requestId;

    @NotBlank
    @Size(max = 50)
    @JsonProperty("username")
    private String userName;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("externalToken")
    private String externalToken;
}
