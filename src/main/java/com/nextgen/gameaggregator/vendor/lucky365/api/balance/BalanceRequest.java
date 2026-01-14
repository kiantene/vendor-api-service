package com.nextgen.gameaggregator.vendor.lucky365.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceRequest {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("SN")
    private String sn;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("ID")
    private String id;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("Method")
    private String method;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("LoginId")
    private String loginId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("Signature")
    private String signature;
}