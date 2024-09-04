package com.nextgen.gameaggregator.vendor.cg.api.authentication;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticationDto {
    @NotBlank
    public String version;

    @NotBlank
    public String channelId;

    @NotBlank
    public String data;
}
