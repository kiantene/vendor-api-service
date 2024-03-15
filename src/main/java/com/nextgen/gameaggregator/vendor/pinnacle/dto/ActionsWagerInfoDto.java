package com.nextgen.gameaggregator.vendor.pinnacle.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.operator.sport.refund.SportRefundData;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import com.nextgen.gameaggregator.operator.sport.unsettle.SportUnsettleData;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionsWagerInfoDto implements SportBetResultData, SportRefundData, SportUnsettleData {
    private String vendorPlayerUsername;

    @JsonProperty("WagerId")
    private Long wagerId;

    @JsonProperty("Type")
    private String type;

    @JsonProperty("BetType")
    private Long betType;

    @JsonProperty("Odds")
    private BigDecimal odds;

    @JsonProperty("OddsFormat")
    private Long oddsFormat;

    @JsonProperty("ToWin")
    private BigDecimal toWin;

    @JsonProperty("ToRisk")
    private BigDecimal toRisk;

    @JsonProperty("Stake")
    private BigDecimal stake;

    @JsonProperty("Period")
    private Long period;

    @JsonProperty("Selection")
    private String selection;

    @JsonProperty("EventId")
    private Long eventId;

    @JsonProperty("EventName")
    private String eventName;

    @JsonProperty("EventDateFm")
    private String eventDateFm;

    @JsonProperty("LeagueId")
    private Long leagueId;

    @JsonProperty("SportId")
    private Long sportId;

    @JsonProperty("Sport")
    private String sport;

    @JsonProperty("Inplay")
    private Boolean inplay;

    @JsonProperty("InPlayScore")
    private String inPlayScore;

    @JsonProperty("Handicap")
    private BigDecimal handicap;

    @JsonProperty("SelectionType")
    private String selectionType;

    @JsonProperty("LeagueName")
    private String leagueName;

    @JsonProperty("ParentEventName")
    private String parentEventName;

    @JsonProperty("PlayerIPAddress")
    private String playerIPAddress;

    @JsonProperty("Legs")
    private List<ActionsWagerInfoLegsDto> legs;

    @JsonProperty("WagerMasterId")
    private Long wagerMasterId;

    @JsonProperty("WagerNum")
    private Integer wagerNum;

    @JsonProperty("RoundRobinOptions")
    private List<String> roundRobinOptions;

    @Override
    public String getExternalTransactionId() {
        return wagerId.toString();
    }

    @Override
    public String getVendorBetId() {
        return wagerId.toString();
    }

    @Override
    public String getRoundId() {
        return wagerId.toString();
    }

    @Override
    public String getGameId() {
        return sportId.toString();
    }

    @Override
    public BigDecimal getBetAmount() {
        return stake;
    }

    @Override
    public BigDecimal getWinAmount() {
        return toWin;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return stake;
    }

    @Override
    public Long getVendorBetTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getResultTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getVendorSettleTime() {
        return System.currentTimeMillis();
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }

    @Override
    public String getVendorPlayerUsername() {
        return vendorPlayerUsername;
    }

    @Override
    public Long getTimestamp() {
        return System.currentTimeMillis();
    }

    @Override
    public BigDecimal getNewBetAmount() {
        return stake;
    }

    @Override
    public Integer getBetType() {
        return this.getType().equalsIgnoreCase("PARLAY") ? BetType.PARLAY_BET.code : BetType.NORMAL_BET.code;
    }

}
