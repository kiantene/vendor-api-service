package com.nextgen.gameaggregator.entity;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

@Document
@Scope("raw")
@Collection("game_session")
@Data
public class GameSession {
    @Id
    private String id;
    private String token;
    private Integer agentId;
    private Long agentPlayerId;
    private String agentPlayerUsername;
    private String vendorPlayerUsername;
    private Long vendorPlayerId;
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
}
