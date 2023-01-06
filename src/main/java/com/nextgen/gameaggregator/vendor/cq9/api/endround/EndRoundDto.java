package com.nextgen.gameaggregator.vendor.cq9.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndRoundDto {
    private String account;
    private String gamehall;
    private String gamecode;
    private String roundid;
    private EndRoundDataDto data;
    private String createTime;
    private BigDecimal freegame;
    private BigDecimal bonus;
    private BigDecimal luckydraw;
    private BigDecimal jackpot;
    private List<BigDecimal> jackpotcontribution;
}
