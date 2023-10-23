package com.nextgen.gameaggregator.vendor.yesbingo.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BalanceDto {

    // Already validated in GeneralAction. Action id to get balance
    public Integer action;

    // timestamp
    @NotNull
    @Positive
    public Long ts;

    // player id
    @NotBlank
    @Pattern(regexp = "^[a-z0-9]+$")
    @Size(max = 50)
    public String uid;

    // Vendor's defined currency
    @NotBlank
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[a-zA-Z]+")
    public String currency;

}
