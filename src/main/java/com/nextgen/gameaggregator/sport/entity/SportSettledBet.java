package com.nextgen.gameaggregator.sport.entity;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetInformation;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import java.math.BigDecimal;

@Document
@Scope("raw")
@Collection("sport_settled_bet")
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class SportSettledBet extends BetInformation {
    private BigDecimal newBetAmount;
    private String vendorPlayerUsername;

    public SportSettledBet(SportUnsettledBetCouchbase sportUnsettledBetCouchbase) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(sportUnsettledBetCouchbase, this);

        this.vendorPlayerUsername = sportUnsettledBetCouchbase.getVendorPlayerUsername();
        this.newBetAmount = sportUnsettledBetCouchbase.getNewBetAmount();
    }

    public String generateId() {
        return this.getVendorPlayerUsername() + '_' + this.getExternalTransactionId();
    }

    public BetHistory toBetHistory(Integer betStatus, Integer resultType) {
        BetHistory betHistory = new BetHistory();
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(this, betHistory);

        betHistory.setId(this.getBetId());
        betHistory.setStatus(betStatus);
        betHistory.setResultType(resultType);

        return betHistory;
    }

    public SportUnsettledBetCouchbase toSportUnsettleBetCouchbase() {
        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = new SportUnsettledBetCouchbase();
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(this, sportUnsettledBetCouchbase);

        return sportUnsettledBetCouchbase;
    }
}
