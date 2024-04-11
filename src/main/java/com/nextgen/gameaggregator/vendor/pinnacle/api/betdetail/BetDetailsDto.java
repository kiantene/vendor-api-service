package com.nextgen.gameaggregator.vendor.pinnacle.api.betdetail;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BetDetailsDto {
    private Long wagerId;
    private Long eventId;
    private String eventName;
    private String parentEventName;
    private String headToHead;
    private String wagerDateFm;
    private String eventDateFm;
    private String settleDateFm;
    private String resettleDateFm;
    private String status;
    private String homeTeam;
    private String awayTeam;
    private String selection;
    private BigDecimal handicap;
    private BigDecimal odds;
    private Integer oddsFormat;
    private Integer betType;
    private String league;
    private Long leagueId;
    private BigDecimal stake;
    private Integer sportId;
    private String sport;
    private String currencyCode;
    private String inplayScore;
    private Boolean inPlay;
    private String homePitcher;
    private String awayPitcher;
    private String homePitcherName;
    private String awayPitcherName;
    private String period;
    private String cancellationStatus;
    private List<?> parlaySelections;
    private String category;
    private BigDecimal toWin;
    private BigDecimal toRisk;
    private String product;
    private String isResettle;
    private BigDecimal parlayMixOdds;
    private BigDecimal parlayFinalOdds;
    private String wagerType;
    private List<?> competitors;
    private String userCode;
    private String loginId;
    private BigDecimal winLoss;
    private BigDecimal turnover;
    private List<?> scores;
    private String result;
    private BigDecimal volume;
    private String view;
}
