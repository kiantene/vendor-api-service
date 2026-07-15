package com.nextgen.gameaggregator.vendor.wazdan.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetResultRequest {

    @NotNull
    private Integer type;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 20, fraction = 2)
    private BigDecimal amount;

    @Valid
    @NotNull
    private User user;

    @Valid
    @NotNull
    private Round round;

    @Valid
    private JackpotInfo jackpotInfo;

    @Valid
    private FreeRoundInfo freeRoundInfo;

    @Valid
    private CashDropInfo cashDropInfo;

    @Valid
    private RoundInfo roundInfo;

    @NotBlank
    @Size(max = 255)
    private String transactionId;

    @NotBlank
    @Size(max = 255)
    private String roundId;

    @NotNull
    private Integer gameId;

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
    public static class Round {
        @NotBlank
        private String betTransactionId;

        @NotNull
        private Boolean endRound;

        private Boolean lastFreeSpin;

        private Boolean lastFreeRound;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JackpotInfo {
        @NotBlank
        private String promotionId;

        @Valid
        private List<CounterInfo> countersInfo;

        @Getter
        @Builder
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class CounterInfo {
            @NotBlank
            private String name;

            @NotNull
            private BigDecimal contribution;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FreeRoundInfo {
        @NotNull
        @PositiveOrZero
        @Digits(integer = 20, fraction = 8)
        private BigDecimal totalBetAmount;

        @NotNull
        @PositiveOrZero
        @Digits(integer = 20, fraction = 8)
        private BigDecimal totalWinAmount;

        @NotNull
        private Integer count;

        @NotNull
        private Integer id;

        @NotBlank
        private String txId;

        private String campaignId;

        private String packageId;

        private String meta;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CashDropInfo {
        @NotNull
        private Integer promotionId;

        @NotNull
        private Integer cashDropId;

        @NotNull
        private Integer prizeId;

        private String meta;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoundInfo {
        private List<String> details;
    }

    @AssertTrue(message = "transactionId and roundId must not contain spaces")
    public boolean isTransactionIdAndRoundIdValid() {
        return transactionId != null && roundId != null
                && !transactionId.contains(" ")
                && !roundId.contains(" ");
    }

    @AssertTrue(message = "Type must be one of: 0, 1, 2, 3, 4, 5")
    public boolean isTypeValid() {
        return type != null && Set.of(0, 1, 2, 3, 4, 5).contains(type);
    }
}
