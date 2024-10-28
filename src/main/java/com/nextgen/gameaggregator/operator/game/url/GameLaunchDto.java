package com.nextgen.gameaggregator.operator.game.url;

import lombok.Data;

@Data
public class GameLaunchDto {

    private String traceId;
    private Integer agentId;
    private Integer masterAgentId;
    private Integer houseId;
    private Integer productId;
    private Integer productGameId;
    private Integer gameCategoryId;
    private Integer currencyId;
    private String currencyCode;
    private Integer platformId;
    private Integer languageId;
    private Integer vendorId;
    private Integer vendorGameId;
    private Long agentPlayerId;
    private String agentPlayerUsername;
    private Long vendorPlayerId;
    private String vendorPlayerUsername;
    private String openGameCode;
    private Integer vendorLineId;
}
