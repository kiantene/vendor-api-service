package com.nextgen.gameaggregator.vendor.casinogate.api.refund;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nextgen.gameaggregator.vendor.casinogate.util.StrictLongDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundRequest {
    @NotNull
    @Positive
    @JsonDeserialize(using = StrictLongDeserializer.class)
    private Long amount;

    /**
     * Id of this refund operation. Maps to {@code BetRollbackContext.idempotencyKey}.
     */
    @NotBlank
    @Size(max = 255)
    private String refundTransactionId;

    @NotBlank
    @Size(max = 255)
    private String roundId;

    @NotBlank
    @Size(max = 255)
    private String token;

    /**
     * Id of the original bet being refunded. Maps to {@code BetRollbackContext.vendorBetId}
     * and is the BY_BET lookup key.
     */
    @NotBlank
    @Size(max = 255)
    private String transactionId;

    // Injected by CasinoGateSignatureValidator via enrichRequestFields.
    private String vendorPlayerUsername;
}
