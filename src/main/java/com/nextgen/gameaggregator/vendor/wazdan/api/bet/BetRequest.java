package com.nextgen.gameaggregator.vendor.wazdan.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetRequest {

    @NotNull
    @Valid
    private User user;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 20, fraction = 2)
    private BigDecimal amount;

    private Boolean freeSpin;

    private Boolean freeRound;

    private Boolean walletFreeSpin;

    private Double baseStake;

    private List<@NotBlank String> tags;

    @NotNull
    private Integer gameId;

    @NotBlank
    @Size(max = 255)
    private String roundId;

    @NotBlank
    @Size(max = 255)
    private String transactionId;

    @Valid
    private JackpotInfo jackpotInfo;

    @Valid
    private FreeRoundInfo freeRoundInfo;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        @NotBlank
        @Size(max = 255)
        private String id;

        private String skinId;

        @NotBlank
        @Size(max = 255)
        private String token;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JackpotInfo {
        @Valid
        private List<CounterInfo> countersInfo;

        private String meta;

        private String promotionId;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class CounterInfo {
            @NotBlank
            private String name;

            @NotNull
            private Double contribution;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FreeRoundInfo {
        private Double betAmount;

        private Integer id;

        private String txId;

        private String campaignId;

        private String packageId;
    }

    @AssertTrue(message = "transactionId and roundId must not contain spaces")
    public boolean isTransactionIdAndRoundIdValid() {
        return transactionId != null && roundId != null
                && !transactionId.contains(" ")
                && !roundId.contains(" ");
    }
}
