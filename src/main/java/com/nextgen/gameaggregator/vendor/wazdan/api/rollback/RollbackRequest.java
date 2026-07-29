package com.nextgen.gameaggregator.vendor.wazdan.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackRequest {

    @Valid
    @NotNull
    private User user;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 20, fraction = 2)
    private BigDecimal amount;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 20, fraction = 2)
    private BigDecimal bonusAmount;

    @NotNull
    private Integer gameId;

    @NotBlank
    @Size(max = 255)
    private String roundId;

    @NotBlank
    @Size(max = 255)
    private String originalTransactionId;

    @NotBlank
    @Size(max = 255)
    private String transactionId;

    @Data
    public static class User {

        @NotBlank
        @Size(max = 255)
        private String id;

        @Size(max = 255)
        private String skinId;

        @NotBlank
        @Size(max = 255)
        private String token;
    }

    @AssertTrue(message = "transactionId, originalTransactionId and roundId must not contain spaces")
    public boolean isTransactionIdAndRoundIdValid() {
        return transactionId != null && originalTransactionId != null && roundId != null
                && !transactionId.contains(" ")
                && !originalTransactionId.contains(" ")
                && !roundId.contains(" ");
    }
}
