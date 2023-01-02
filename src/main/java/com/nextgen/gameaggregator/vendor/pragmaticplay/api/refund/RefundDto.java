package com.nextgen.gameaggregator.vendor.pragmaticplay.api.refund;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.operator.wallet.win.WinData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundDto {

    // Hash code of the request
    @NotBlank
    private String hash;

    // Identifier of the user within the Casino Operator’s system.
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String userId;

    // Unique reference of this transaction.
    @NotBlank
    @Size(min = 1, max = 32)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String reference;

    // Game Provider id.
    @NotBlank
    private String providerId;

    // Token of the player from Authenticate response.
    @NotBlank
    private String token;

    public String getExternalTransactionId() {
        return this.reference;
    }
}
