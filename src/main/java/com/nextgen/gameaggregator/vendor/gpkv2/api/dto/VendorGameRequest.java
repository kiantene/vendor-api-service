package com.nextgen.gameaggregator.vendor.gpkv2.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorGameRequest {

    @JsonProperty("action")
    private String action;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("operator_player_id")
    private String operatorPlayerId;

    @JsonProperty("session_token")
    private String sessionToken;

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("round_id")
    private String roundId;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("game_token")
    private String gameToken;

    @JsonProperty("finished")
    private String finished;

    @JsonProperty("gameCategories")
    private Integer gameCategories;

    @JsonProperty("bet_transaction_id")
    private String betTransactionId;

}

