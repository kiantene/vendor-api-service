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

}
