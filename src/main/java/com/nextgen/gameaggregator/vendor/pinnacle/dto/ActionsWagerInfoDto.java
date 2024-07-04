package com.nextgen.gameaggregator.vendor.pinnacle.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ActionsWagerInfoDto {
    @JsonIgnore
    private String vendorPlayerUsername;
    @JsonIgnore
    private String transactionDate;

    private Long wagerId;
    private String type;
    private Long betTypes;
    private BigDecimal odds;
    private Long oddsFormat;
    private BigDecimal toWin;
    private BigDecimal toRisk;
    private BigDecimal stake;
    private BigDecimal profitAndLoss;
    private Long period;
    private String selection;
    private Long eventId;
    private String eventName;
    private String eventDateFm;
    private String settlementTime;
    private String resettlementTime;
    private Long leagueId;
    private Long sportId;
    private String sport;
    private Boolean inplay;
    private String inPlayScore;
    private BigDecimal handicap;
    private String selectionType;
    private String leagueName;
    private String parentEventName;
    private String playerIPAddress;
    private List<ActionsWagerInfoLegsDto> legs;
    private Long wagerMasterId;
    private Integer wagerNum;
    private List<String> roundRobinOptions;
}
