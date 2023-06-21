package com.nextgen.gameaggregator.entity;

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

@Document
@Scope("raw")
@Collection("settled_bet")
@Data
@NoArgsConstructor
public class SettledBet extends BetInformation {

    public SettledBet(BetResultData betResultData) {
        super(betResultData);
        this.setStatus(BetStatus.SETTLED.code);
    }

    public SettledBet(UnsettledBet unsettledBet, BaseVendorService vendorService) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(unsettledBet, this);

        if (unsettledBet.getWinAmount() == null) this.setWinAmount(BigDecimal.ZERO);
        if (unsettledBet.getJackpotAmount() == null) this.setJackpotAmount(BigDecimal.ZERO);
        if (unsettledBet.getIsFreespin() == null) this.setIsFreespin(0);

        this.setWinLoss(vendorService.calculateWinLoss(this));
        this.setEffectiveTurnover(vendorService.calculateEffectiveTurnover(this));
        this.setStatus(BetStatus.SETTLED.code);
    }

    public SettledBet(EndRoundSettledBet endRoundSettledBet) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(endRoundSettledBet, this);
    }
}
