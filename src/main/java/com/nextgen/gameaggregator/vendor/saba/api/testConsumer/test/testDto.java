package com.nextgen.gameaggregator.vendor.saba.api.testConsumer.test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class testDto implements SportBetResultData {

    private String id;
    private String betId;
    private String internalTransactionId;
    private String externalTransactionId;
    private String vendorBetId;
    private String roundId;
    private Integer vendorGameId;
    private Long vendorPlayerId;
    private Integer vendorId;
    private Integer vendorLineId;
    private Long agentPlayerId;
    private Integer agentId;
    private Integer operatorStatus;
    private Integer currencyId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal jackpotAmount;
    private BigDecimal winLoss;
    private BigDecimal effectiveTurnover;
    private Integer resultType;
    private Integer isFreespin;
    private String rawData;
    private Integer resettleNum;
    private Integer status;
    private String gameSessionToken;
    private Integer gameCategoryId;
    private Long vendorBetTime;
    private Long vendorSettleTime;
    private Long createTime;
    private Long resultTime;
    private Integer processingStatus;
    private BigDecimal balance;
    private Integer betType;
    private BigDecimal newBetAmount;
    private String vendorPlayerUsername;
    private Integer isConfirmBet;

    @Override
    public String getGameId() {
        return "1";
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }
}
