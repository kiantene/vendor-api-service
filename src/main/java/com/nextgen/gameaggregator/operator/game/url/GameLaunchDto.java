package com.nextgen.gameaggregator.operator.game.url;

import com.nextgen.gameaggregator.entity.ga.VendorLine;
import lombok.Data;

@Data
public class GameLaunchDto {

    private String traceId;
    private Integer agentId;
    private Integer masterAgentId;
    private Integer houseId;
    private String productCode;
    private Integer productId;
    private Integer productGameId;
    private Integer gameCategoryId;
    private Integer currencyId;
    private String currencyCode;
    private String vendorCurrencyCode;
    private Integer platformId;
    private String vendorPlatformCode;
    private Integer languageId;
    private String vendorLanguageCode;
    private Integer vendorId;
    private Integer vendorGameId;
    private Long agentPlayerId;
    private String agentPlayerUsername;
    private Long vendorPlayerId;
    private String vendorPlayerUsername;
    private String openGameCode;
    private Integer vendorLineId;
    private VendorLine vendorLine;
    private Boolean isLaunchByProductGame;
}
