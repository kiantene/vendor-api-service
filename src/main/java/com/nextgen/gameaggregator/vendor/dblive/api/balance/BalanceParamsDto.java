package com.nextgen.gameaggregator.vendor.dblive.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceParamsDto {
    @NotBlank
    @Size(max = 50)
    private String loginName;
    @NotBlank
    @Size(max = 3)
    private String currency;

}
