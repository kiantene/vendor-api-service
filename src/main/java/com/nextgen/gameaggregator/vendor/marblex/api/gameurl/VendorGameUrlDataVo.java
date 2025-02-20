package com.nextgen.gameaggregator.vendor.marblex.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorGameUrlDataVo {

    @NotBlank
    @JsonProperty("GameUrl")
    private String gameUrl;
}
