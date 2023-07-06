package com.nextgen.gameaggregator.vendor.yesbingo.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BalanceDto {

    // an id to request balance
    @NotBlank
    @Size(max = 32)
    @Positive
    public int action;

    // timestamp
    @NotBlank
    @Positive
    public long ts;

    // player id
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(max = 50)
    public String uid;

    // Vendor's defined currency
    @NotBlank
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[a-zA-Z]+")
    public String currency;

}
