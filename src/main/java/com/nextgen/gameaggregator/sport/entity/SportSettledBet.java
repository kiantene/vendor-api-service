package com.nextgen.gameaggregator.sport.entity;

import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.entity.ga.BetInformation;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.enums.BetResultType;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.sport.adjustment.SportAdjustmentData;
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

    public SportSettledBet(String traceId, VendorPlayer vendorPlayer, AgentPlayer agentPlayer, SportAdjustmentData sportAdjustmentData, String rawData) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(sportAdjustmentData, this);

        //TODO retrieve vendor_game_id and game_category_id
        this.setVendorGameId(999);
        this.setGameCategoryId(999);

        this.setBetId(traceId);
        this.setInternalTransactionId(traceId);
        this.setVendorPlayerId(vendorPlayer.getId());
        this.setVendorId(vendorPlayer.getVendorId());
        this.setAgentPlayerId(agentPlayer.getId());
        this.setAgentId(agentPlayer.getAgentId());
        this.setVendorLineId(vendorPlayer.getVendorLineId());
        this.setCurrencyId(vendorPlayer.getCurrencyId());
        this.setGameSessionToken("");

        this.setIsFreespin(0);
        this.setBetAmount(BigDecimal.ZERO);
        this.setWinAmount(sportAdjustmentData.getAmount());
        this.setJackpotAmount(BigDecimal.ZERO);
        this.setWinLoss(sportAdjustmentData.getAmount());
        this.setEffectiveTurnover(BigDecimal.ZERO);
        this.setIsFreespin(0);

        this.setResultType(BetResultType.ADJUSTMENT.code);
        this.setRawData(rawData);
        this.setResettleNum(0);
        this.setStatus(BetStatus.SETTLED.code);

        this.setVendorBetTime(sportAdjustmentData.getTimestamp());
        this.setVendorSettleTime(sportAdjustmentData.getTimestamp());
        this.setResultTime(sportAdjustmentData.getTimestamp());

        this.setId(this.generateId());
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
