package com.nextgen.gameaggregator.entity;

import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

@Document
@Scope("raw")
@Collection("settled_bet")
@Data
@NoArgsConstructor
public class SettledBet extends BetInformation {
    public SettledBet(UnsettledBet unsettledBet) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.map(unsettledBet, this);
    }

    public SettledBet(BetResultData betResultData) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.map(betResultData, this);
    }
}
