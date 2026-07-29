package com.nextgen.gameaggregator.vendor.mtlive.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceRequest {

    @NotBlank
    @Size(max = 10)
    private String system_code;

    @NotBlank
    @Size(max = 15)
    private String web_id;

    @NotBlank
    @Size(max = 20)
    private String user_id;

    @NotBlank
    private String msg;
}