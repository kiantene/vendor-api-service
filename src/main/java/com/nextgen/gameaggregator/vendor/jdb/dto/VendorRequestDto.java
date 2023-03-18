package com.nextgen.gameaggregator.vendor.jdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorRequestDto {
    @JsonProperty("x")
    @NotBlank
    private String x;
}
