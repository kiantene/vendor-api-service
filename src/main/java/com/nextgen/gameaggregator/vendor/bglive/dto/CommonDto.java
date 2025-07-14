package com.nextgen.gameaggregator.vendor.bglive.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto {

    @NotBlank
    @JsonProperty("method")
    private String method;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("id")
    private String id;

    @JsonProperty("params")
    private CommonParamsDto commonParamsDto;
}
