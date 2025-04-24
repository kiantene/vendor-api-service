package com.nextgen.gameaggregator.vendor.ygg.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerInfoDto {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("sessiontoken")
    private String sessionToken;

    @NotBlank
    @Size(max = 255)
    private String org;

}
