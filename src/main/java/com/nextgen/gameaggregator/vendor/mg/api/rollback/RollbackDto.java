package com.nextgen.gameaggregator.vendor.mg.api.rollback;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RollbackDto {
    @NotBlank
    @Size(max = 50)
    private String playerId;
    
    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    @Size(max = 3)
    private String currency;

    @NotBlank
    @Size(max = 256)
    private String txnId;

    @Size(max = 50)
    @Pattern(regexp = "^[A-Za-z0-9_,~().!\\*'\\:@;-]*$")
    private String extOperatorToken;
}
