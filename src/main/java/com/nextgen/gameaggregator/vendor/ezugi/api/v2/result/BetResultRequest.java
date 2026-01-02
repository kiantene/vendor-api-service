package com.nextgen.gameaggregator.vendor.ezugi.api.v2.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetResultRequest {

    @JsonProperty("gameId")
    private Integer gameId;

    @JsonProperty("debitTransactionId")
    private String debitTransactionId;

    @JsonProperty("gameDataString")
    @JsonDeserialize(using = GameDataStringDeserializer.class)
    private GameDataString gameDataString;

    @JsonProperty("isEndRound")
    private boolean endRound;

    @JsonProperty("creditIndex")
    private String creditIndex;

    @JsonProperty("platformId")
    private Integer platformId;

    @JsonProperty("serverId")
    private Integer serverId;

    @NotBlank
    @Size(min = 1, max = 100)
    @JsonProperty("transactionId")
    private String transactionId;

    @NotBlank
    @JsonProperty("token")
    private String token;

    @NotBlank
    @Size(min = 1, max = 50)
    @JsonProperty("uid")
    private String uid;

    @JsonProperty("returnReason")
    private Integer returnReason;

    @JsonProperty("betTypeID")
    private Integer betTypeId;

    @JsonProperty("tableId")
    private Integer tableId;

    @JsonProperty("seatId")
    private String seatId;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("creditAmount")
    private BigDecimal creditAmount;

    @JsonProperty("operatorId")
    private Integer operatorId;

    @NotNull
    @Digits(integer = 18, fraction = 0)
    @JsonProperty("roundId")
    private BigInteger roundId;

    @JsonProperty("timestamp")
    private Long timestamp;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GameDataString {

        @JsonProperty("TableId")
        private String tableId;

        @JsonProperty("PlayerId")
        private String playerId;

        @JsonProperty("CardInShoe")
        private Integer cardInShoe;

        @JsonProperty("DealerCardHandValue")
        private String dealerCardHandValue;

        @JsonProperty("BetTypeId")
        private Integer betTypeId;

        @JsonProperty("BetAmount")
        private BigDecimal betAmount;

        @JsonProperty("InsuranceDecision")
        private String insuranceDecision;

        @JsonProperty("SessionCurrency")
        private String sessionCurrency;

        // @JsonProperty("PlayerCards")
        // private List<PlayerCard> playerCards;

        @JsonProperty("TakenSeatsNumber")
        private Integer takenSeatsNumber;

        @JsonProperty("OperatorID")
        private String operatorID;

        // @JsonProperty("DealerCards")
        // private List<DealerCard> dealerCards;

        @JsonProperty("PlayerCardHandValue")
        private String playerCardHandValue;

        @JsonProperty("BetsList")
        @JsonDeserialize(using = BetsListDeserializer.class) // Custom deserializer for BetsList to handle both array and object formats
        private List<Bet> betsList;

        @JsonProperty("ServerId")
        private Integer serverId;

        @JsonProperty("Version")
        private String version;

        @JsonProperty("DealerId")
        private String dealerId;

        @JsonProperty("GameResults")
        private String gameResults;

        @JsonProperty("roundId")
        private BigInteger roundId;

        @JsonProperty("WinAmount")
        private BigDecimal winAmount;

        @JsonProperty("TotalWin")
        private BigDecimal totalWin;

        @JsonProperty("WinningBets")
        @JsonDeserialize(using = WinningBetsDeserializer.class) // Custom deserializer for WinningBets
        private Map<String, BigDecimal> winningBets;

        @JsonProperty("GameID")
        private Integer gameID;

        @JsonProperty("SeatId")
        private String seatId;

        @JsonProperty("commission")
        private Integer commission;
    }

    // @Data
    // @JsonIgnoreProperties(ignoreUnknown = true)
    // public static class PlayerCard {
    //     @JsonProperty("CardName")
    //     private String cardName;

    //     @JsonProperty("CardValue")
    //     private Integer cardValue;
    // }

    // @Data
    // @JsonIgnoreProperties(ignoreUnknown = true)
    // public static class DealerCard {
    //     @JsonProperty("CardName")
    //     private String cardName;

    //     @JsonProperty("CardValue")
    //     private Integer cardValue;
    // }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bet {
        @JsonProperty("BetName")
        private String betName;

        @JsonProperty("BetAmount")
        private BigDecimal betAmount;
    }
}
