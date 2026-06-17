package com.nextgen.gameaggregator.vendor.egtdigital.api.balance;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.egtdigital.dto.RequestCommonDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BalanceRequest extends RequestCommonDto {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("currency")
    private String currency;
}
