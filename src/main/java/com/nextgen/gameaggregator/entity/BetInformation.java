package com.nextgen.gameaggregator.entity;

import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
public abstract class BetInformation {
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
    public BetInformation(BetResultData betResultData) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(betResultData, this);

        if (this.internalTransactionId == null) this.internalTransactionId = UUID.randomUUID().toString();
        if (this.isFreespin == null) this.isFreespin = 0;
        if (this.betAmount == null) this.betAmount = BigDecimal.ZERO;
        if (this.winAmount == null) this.winAmount = BigDecimal.ZERO;
        if (this.jackpotAmount == null) this.jackpotAmount = BigDecimal.ZERO;
        if (this.winLoss == null) this.winLoss = BigDecimal.ZERO;
        if (this.effectiveTurnover == null) this.effectiveTurnover = BigDecimal.ZERO;
    }
}
