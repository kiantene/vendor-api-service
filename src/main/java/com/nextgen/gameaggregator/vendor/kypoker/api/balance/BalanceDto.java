package com.nextgen.gameaggregator.vendor.kypoker.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {

    @NotBlank
    @Size(min = 1, max = 36)
    private String account;

    @NotBlank
    @Size(min = 1, max = 4)
    private String currency;

    @NotBlank
    @Size(min = 1, max = 36)
    private String s;

}
