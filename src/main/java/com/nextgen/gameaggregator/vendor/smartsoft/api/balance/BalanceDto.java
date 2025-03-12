package com.nextgen.gameaggregator.vendor.smartsoft.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {
    @NotBlank
    private String signature;

    @NotBlank
    private String sessionId;

    @NotBlank
    private String userName;

    @NotBlank
    private String clientExternalKey;

}
