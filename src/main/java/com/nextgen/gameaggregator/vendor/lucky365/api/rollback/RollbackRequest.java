package com.nextgen.gameaggregator.vendor.lucky365.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackRequest {
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

    @NotBlank
    @Size(max = 255)
    @JsonProperty("OrderCode")
    private String orderCode;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("ActionDate")
    private String actionDate;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("GameName")
    private String gameName;

}