package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

@Document
@Scope("raw")
@Collection("game_sessions")
@Data
@NoArgsConstructor
public class GameSession {
    @Id
    private String id;
    private String token;
    private String vendorToken;
    private Integer agentId;
    private Long agentPlayerId;
    private String agentPlayerUsername;
    private String vendorPlayerUsername;
    private Long vendorPlayerId;
    private String productCode;
    private Integer productId;
    private Integer productGameId;
    private Integer vendorId;
    private Integer vendorLineId;
    private String gameCode;
    private Integer vendorGameId;
    private String vendorGameCode;
    private String vendorCurrencyCode;
    private Integer gameCategoryId;
    private Integer languageId;
    private String language;
    private String vendorLanguageCode;
    private Integer currencyId;
    private String currencyCode;
    private String vendorPlatformCode;
    private Integer platformId;
    private String traceId;
    private Integer status;
    private String lobbyUrl;
    private String ipAddress;
    private Long createTime;
    private Long terminateTime;
    private Boolean isLaunchByProductGame;

    public GameSession(EndRoundSettledBet endRoundSettledBet) {
        this.agentPlayerUsername = endRoundSettledBet.getAgentPlayerUsername();
        this.currencyCode = endRoundSettledBet.getCurrencyCode();
        this.gameCode = endRoundSettledBet.getGameCode();
        this.token = endRoundSettledBet.getGameSessionToken();
        this.vendorId = endRoundSettledBet.getVendorId();
        this.currencyId = endRoundSettledBet.getCurrencyId();
    }

    /**
     * Copy constructor. All fields are scalar (immutable String/Integer/Long/Boolean),
     * so a field-by-field copy is a safe deep copy.
     * <p>
     * Used to build a short-lived, request-local validation view of a shared/cached
     * session without mutating the original (see {@code BetValidator}). This keeps
     * concurrent requests on the same session from corrupting each other.
     */
    public GameSession(GameSession other) {
        this.id = other.id;
        this.token = other.token;
        this.vendorToken = other.vendorToken;
        this.agentId = other.agentId;
        this.agentPlayerId = other.agentPlayerId;
        this.agentPlayerUsername = other.agentPlayerUsername;
        this.vendorPlayerUsername = other.vendorPlayerUsername;
        this.vendorPlayerId = other.vendorPlayerId;
        this.productCode = other.productCode;
        this.productId = other.productId;
        this.productGameId = other.productGameId;
        this.vendorId = other.vendorId;
        this.vendorLineId = other.vendorLineId;
        this.gameCode = other.gameCode;
        this.vendorGameId = other.vendorGameId;
        this.vendorGameCode = other.vendorGameCode;
        this.vendorCurrencyCode = other.vendorCurrencyCode;
        this.gameCategoryId = other.gameCategoryId;
        this.languageId = other.languageId;
        this.language = other.language;
        this.vendorLanguageCode = other.vendorLanguageCode;
        this.currencyId = other.currencyId;
        this.currencyCode = other.currencyCode;
        this.vendorPlatformCode = other.vendorPlatformCode;
        this.platformId = other.platformId;
        this.traceId = other.traceId;
        this.status = other.status;
        this.lobbyUrl = other.lobbyUrl;
        this.ipAddress = other.ipAddress;
        this.createTime = other.createTime;
        this.terminateTime = other.terminateTime;
        this.isLaunchByProductGame = other.isLaunchByProductGame;
    }
}
