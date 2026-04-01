package com.nextgen.gameaggregator.vendor.digitain.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticateRequest {

    @NotNull
    @JsonProperty("prid")
    private Integer prid;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("tkn")
    private String tkn;

}
