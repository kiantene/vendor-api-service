package com.nextgen.gameaggregator.core.engine.promo.payout;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Builder
@Getter
public class PromoPayoutRequest {
    /**
     * A unique identifier for tracing the request throughout the system.
     * This can be used for logging, debugging, and correlating related operations.
     * Client system can use this ID to trace requests across different systems/services/components.
     * This value will remain the same during retries.
     */
    @NotBlank(message = "Trace ID is required and cannot be blank.")
    @Size(min = 1, max = 64, message = "Trace ID must be between 1 and 64 characters.")
    private final String traceId;

    /**
     * The username of the user for whom the promotion payout is being made.
     */
    @NotBlank(message = "Username is required and cannot be blank.")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters.")
    private final String username;

    /**
     * A unique identifier for the specific payout transaction.
     * Client system should use this ID for idempotency checks.
     * This value will remain the same during retries.
     */
    @NotBlank(message = "Transaction ID is required and cannot be blank.")
    @Size(min = 1, max = 128, message = "Transaction ID must be between 1 and 128 characters.")
    private final String transactionId;

    /**
     * The currency in which the promotion payout is being made (e.g., "USD", "SGD", "EUR").
     */
    @NotBlank(message = "Currency is required and cannot be blank.")
    private final String currency;

    /**
     * The amount of the promotion payout.
     */
    @NotNull(message = "Amount is required.")
    @Positive
    private final BigDecimal amount;

    /**
     * The type of promotion payout (e.g., "BONUS", "REBATE", "CASHBACK").
     */
    @NotBlank(message = "Payout type is required and cannot be blank.")
    private final String type;

    /**
     * The timestamp indicating when the promotion payout request was initiated, in milliseconds since the Unix epoch.
     */
    @NotNull(message = "Timestamp is required.")
    private final Long timestamp;
}
