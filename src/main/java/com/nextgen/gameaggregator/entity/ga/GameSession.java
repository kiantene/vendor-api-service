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

    public GameSession(EndRoundSettledBet endRoundSettledBet) {
        this.agentPlayerUsername = endRoundSettledBet.getAgentPlayerUsername();
        this.currencyCode = endRoundSettledBet.getCurrencyCode();
        this.gameCode = endRoundSettledBet.getGameCode();
        this.token = endRoundSettledBet.getGameSessionToken();
        this.vendorId = endRoundSettledBet.getVendorId();
        this.currencyId = endRoundSettledBet.getCurrencyId();
    }
}
