package com.nextgen.gameaggregator.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Table(name = "game_sessions")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class GameSession extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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
}
