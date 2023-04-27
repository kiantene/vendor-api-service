package com.nextgen.gameaggregator.vendor.mg.api.getBalance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GetBalanceDto {
    @NotBlank
    @Size(min = 1, max = 50)
    private String playerId;

    @Size(max = 50)
    @Pattern(regexp = "^[A-Za-z0-9_,~().!\\*'\\:@;-]*$")
    private String extOperatorToken;
}
