package com.nextgen.gameaggregator.operator.transactions.detail;

import lombok.Data;

@Data
public class MatchDetailData {

    private String matchName;
    private Long matchDate;
    private String homeTeamName;
    private String awayTeamName;
    private String homeTeamScore;
    private String awayTeamScore;
    private String betTypeName;
    private String betTeam;
    private String handicap;
    private String odds;
    private String betStatus;
    private Long settleDate;

}
