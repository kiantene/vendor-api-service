package com.nextgen.gameaggregator.vendor.jdb.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetNSettleDto {
    private String action;
    private Long ts;
    private Long transferId;
    private Long gameSeqNo;
    private String uid;
    @JsonProperty("gType")
    private Integer gType;
    @JsonProperty("mType")
    private Integer mType;
    private String reportDate;
    private String gameDate;
    private String currency;
    private BigDecimal bet;
    private BigDecimal win;
    private BigDecimal netWin;
    private BigDecimal denom;
    private String ipAddress;
    private String clientType;
    private Integer systemTakeWin;
    private String lastModifyTime;
    private String sessionNo;
    private BigDecimal mb;


    // Slot Only, gType = 0
    private BigDecimal jackpotWin;
    private BigDecimal jackpotContribute;
    @JsonProperty("hasFreegame")
    private Boolean hasFreeGame;;


    // Fish Only, gType = 7
    private String roomType;


    // Slot and Arcade, gType = 0 OR gType = 9
    private Boolean hasGamble;


    // Arcade and Lottery, gType = 9 OR gType = 12
    private Boolean hasBonusGame;
}
