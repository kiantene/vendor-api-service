package com.nextgen.gameaggregator.vendor.bglive.api.settlement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettleDto {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("id")
    private String id;

}
