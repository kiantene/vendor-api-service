package com.nextgen.gameaggregator.entity;

import com.nextgen.gameaggregator.enums.BetResultType;
import com.nextgen.gameaggregator.enums.BetStatus;
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
@Collection("settled_bet")
@Data
@NoArgsConstructor
public class SettledBet extends BetInformation {

    public SettledBet(BetResultData betResultData, String traceId, Integer vendorGameId, Long vendorPlayerId) {
        super(betResultData);
        this.setVendorGameId(vendorGameId);
        this.setVendorPlayerId(vendorPlayerId);
        this.setId(this.generateId());
        this.setInternalTransactionId(traceId);
        this.setStatus(BetStatus.SETTLED.code);
        this.calculateResultType();
        this.setCreateTime(System.currentTimeMillis());
    }

    public SettledBet(UnsettledBet unsettledBet, BaseVendorService vendorService, String traceId) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(unsettledBet, this);

        if (unsettledBet.getWinAmount() == null) this.setWinAmount(BigDecimal.ZERO);
        if (unsettledBet.getJackpotAmount() == null) this.setJackpotAmount(BigDecimal.ZERO);
        if (unsettledBet.getIsFreespin() == null) this.setIsFreespin(0);

        this.setInternalTransactionId(traceId);
        this.setWinLoss(vendorService.calculateWinLoss(this));
        this.setEffectiveTurnover(vendorService.calculateEffectiveTurnover(this));
        this.setStatus(BetStatus.SETTLED.code);
        this.calculateResultType();
        this.setCreateTime(System.currentTimeMillis());
        this.setVendorSettleTime( Optional.ofNullable(unsettledBet.getVendorSettleTime()).orElse(System.currentTimeMillis()) );
    }

    public SettledBet(EndRoundSettledBet endRoundSettledBet) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(endRoundSettledBet, this);
        this.calculateResultType();
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
        } else if (isWinAmountMoreThanZero){
            betResultType = BetResultType.WIN.code;
        }

        this.setResultType(betResultType);
    }
}
