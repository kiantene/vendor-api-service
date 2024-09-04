package com.nextgen.gameaggregator.vendor.cg.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {
    @NotBlank
    @Size(max = 255)
    //@Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    public String channelId;
    @NotBlank
    @Size(max = 50)
    //@Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    public String accountId;
}
