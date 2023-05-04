package com.nextgen.gameaggregator.entity;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

@Document
@Scope("raw")
@Collection("unsettled_bet")
@Data
@NoArgsConstructor
public class UnsettledBet extends BetInformation {

    public UnsettledBet(BetResultData betResultData) {
        super(betResultData);
        this.setStatus(BetStatus.UNSETTLED.code);
    }

    public UnsettledBet(SettledBet settledBet) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(settledBet, this);
    }
}
