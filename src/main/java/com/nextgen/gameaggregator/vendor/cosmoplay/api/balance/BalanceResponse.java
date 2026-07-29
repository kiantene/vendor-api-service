package com.nextgen.gameaggregator.vendor.cosmoplay.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceResponse {
    @NotBlank
    @JsonProperty("GameID")
    private String gameID;

    // This indicates the type of wallet integration a player uses. If set to 'true',
    // the player is using a multiple wallet integration. Conversely, if it's set
    // to 'false', the player uses a seamless wallet integration.
    // ----
    // In our case, this value is always false.
    // ----
    @NotBlank
    @JsonProperty("IsWalletIntegrated")
    private Boolean isWalletIntegrated;

    // Balance is required upon seamless wallet integration.
    @JsonProperty("Balance")
    private Long balance;

    // -------------------------------------------------------------------------------------------------------------- //
    // --------------------------------------------- Not required --------------------------------------------------- //
    // -------------------------------------------------------------------------------------------------------------- //

    @JsonProperty("CoinsMap")
    private List<Integer> coinsMap;

    @JsonProperty("CoinValueMap")
    private List<Integer> coinValueMap;

    @JsonProperty("DefaultCoins")
    private Integer defaultCoins;

    @JsonProperty("DefaultCoinValue")
    private Integer defaultCoinValue;

    @JsonProperty("EnableFreeRounds")
    private Boolean enableFreeRounds;

    @JsonProperty("AwardFreeRounds")
    private Integer awardFreeRounds;

    @JsonProperty("FreeRoundCoins")
    private Integer freeRoundCoins;

    @JsonProperty("FreeRoundCoinVal")
    private Integer freeRoundCoinVal;

    @JsonProperty("DecimalPlace")
    private Integer decimalPlace;
}
