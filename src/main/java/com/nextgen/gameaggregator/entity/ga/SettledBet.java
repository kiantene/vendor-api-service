package com.nextgen.gameaggregator.entity.ga;

import com.nextgen.gameaggregator.enums.BetResultType;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.adjustment.AdjustmentData;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import java.math.BigDecimal;
import java.util.Optional;

@Document
@Scope("raw")
@Collection("settled_bets")
@Data
@NoArgsConstructor
public class SettledBet extends BetInformation {

    public SettledBet(BetInformation betInformation) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(betInformation, this);
    }

    public SettledBet(BetResultData betResultData, String internalTransactionId, Integer vendorGameId, Long vendorPlayerId, GameSession gameSession) {
        super(betResultData);
        this.setVendorGameId(vendorGameId);
        this.setVendorPlayerId(vendorPlayerId);
        this.setId(this.generateId());
        this.setInternalTransactionId(internalTransactionId);
        this.setBetId(internalTransactionId);
        this.setStatus(BetStatus.SETTLED.code);
        this.calculateResultType();
        this.setCreateTime(System.currentTimeMillis());
        this.setGameSessionToken(gameSession.getToken());
        this.setVendorLineId(gameSession.getVendorLineId());
        this.setAgentPlayerId(gameSession.getAgentPlayerId());
        this.setAgentId(gameSession.getAgentId());
        this.setGameCategoryId(gameSession.getGameCategoryId());
        this.setCurrencyId(gameSession.getCurrencyId());
    }

    public SettledBet(UnsettledBet unsettledBet, BaseVendorService vendorService, String traceId) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(unsettledBet, this);

        if (unsettledBet.getWinAmount() == null) this.setWinAmount(BigDecimal.ZERO);
        if (unsettledBet.getJackpotAmount() == null) this.setJackpotAmount(BigDecimal.ZERO);
        if (unsettledBet.getIsFreespin() == null) this.setIsFreespin(0);

        this.setInternalTransactionId(traceId);
        this.setBetId(unsettledBet.getBetId());
        this.setWinLoss(vendorService.calculateWinLoss(this));
        this.setEffectiveTurnover(vendorService.calculateEffectiveTurnover(this));
        this.setStatus(BetStatus.SETTLED.code);
        this.calculateResultType();
        this.setCreateTime(System.currentTimeMillis());
        this.setVendorSettleTime(Optional.ofNullable(unsettledBet.getVendorSettleTime()).orElse(System.currentTimeMillis()));
        this.setGameSessionToken(unsettledBet.getGameSessionToken());
    }

    public SettledBet(EndRoundSettledBet endRoundSettledBet) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(endRoundSettledBet, this);
        this.calculateResultType();
        this.setCreateTime(System.currentTimeMillis());

        //if no result time then will set it as settle time
        if (endRoundSettledBet.getResultTime() == null) {
            this.setResultTime(endRoundSettledBet.getVendorSettleTime());
        }
    }

    public SettledBet(AdjustmentData adjustmentData, String traceId, GameSession gameSession) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(adjustmentData, this);
        this.setBetId(traceId);
        this.setBetAmount(BigDecimal.ZERO);
        this.setWinAmount(adjustmentData.getAdjustmentAmount());
        this.setVendorId(gameSession.getVendorId());
        this.setVendorLineId(gameSession.getVendorLineId());
        this.setVendorGameId(gameSession.getVendorGameId());
        this.setVendorPlayerId(gameSession.getVendorPlayerId());
        this.setAgentPlayerId(gameSession.getAgentPlayerId());
        this.setAgentId(gameSession.getAgentId());
        this.setId(this.generateId());
        this.setInternalTransactionId(traceId);
        this.setCurrencyId(gameSession.getCurrencyId());
        this.setGameCategoryId(gameSession.getGameCategoryId());
        this.setGameSessionToken(gameSession.getToken());
        this.setCreateTime(System.currentTimeMillis());
    }

    public String generateId() {
        return this.getVendorBetId() + '_' + this.getRoundId() + '_' + this.getVendorGameId() + '_' + this.getVendorPlayerId();
    }

    public void calculateResultType() {
        BigDecimal winAmount = Optional.ofNullable(this.getWinAmount()).orElse(BigDecimal.ZERO);
        BigDecimal jackpotAmount = Optional.ofNullable(this.getJackpotAmount()).orElse(BigDecimal.ZERO);

        boolean isWinAmountMoreThanZero = winAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean isJackpotAmountMoreThanZero = jackpotAmount.compareTo(BigDecimal.ZERO) > 0;

        Integer betResultType = BetResultType.LOSE.code;

        if (isJackpotAmountMoreThanZero) {
            betResultType = BetResultType.JACKPOT.code;
        } else if (isWinAmountMoreThanZero) {
            betResultType = BetResultType.WIN.code;
        }

        this.setResultType(betResultType);
    }
}
