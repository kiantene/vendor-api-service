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
     */
    @NotBlank(message = "Trace ID is required and cannot be blank.")
    @Size(min = 1, max = 64, message = "Trace ID must be between 1 and 64 characters.")
    private final String traceId;

    /**
     * The username of the user for whom the promotion payout is being made.
     * This identifies the recipient of the payout.
     */
    @NotBlank(message = "Username is required and cannot be blank.")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters.")
    private final String username;

    /**
     * A unique identifier for the specific payout transaction.
     * This helps in identifying and tracking individual payout events.
     */
    @NotBlank(message = "Transaction ID is required and cannot be blank.")
    @Size(min = 1, max = 128, message = "Transaction ID must be between 1 and 128 characters.")
    private final String transactionId;

    /**
     * The currency in which the promotion payout is being made (e.g., "USD", "SGD", "EUR").
     * This ensures the correct currency is associated with the amount.
     */
    @NotBlank(message = "Currency is required and cannot be blank.")
    private final String currency;

    /**
     * The amount of the promotion payout.
     * Using BigDecimal is recommended for financial calculations to avoid precision issues.
     */
    @NotNull(message = "Amount is required.")
    @Positive
    private final BigDecimal amount;

    /**
     * The type of promotion payout (e.g., "BONUS", "REBATE", "CASHBACK").
     * This categorizes the nature of the payout.
     */
    @NotBlank(message = "Payout type is required and cannot be blank.")
    private final String type;

    /**
     * The timestamp indicating when the promotion payout request was initiated, in milliseconds since the Unix epoch.
     * This provides a chronological record of the request.
     */
    @NotNull(message = "Timestamp is required.")
    private final Long timestamp;
}
