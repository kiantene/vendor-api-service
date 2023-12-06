package com.nextgen.gameaggregator.sport.entity;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetInformation;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
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
@Collection("sport_unsettled_bet")
@Data
@NoArgsConstructor
public class SportUnsettledBetCouchbase extends BetInformation {
    private BigDecimal newBetAmount;
    private GameSession gameSession;

    public SportUnsettledBetCouchbase(GameSession gameSession, String rawData, SportBetResultData sportBetResultData, String traceId, Integer resultType) {
        super(sportBetResultData);

        this.setVendorGameId(gameSession.getVendorGameId());
        this.setVendorPlayerId(gameSession.getVendorPlayerId());
        this.setStatus(BetStatus.UNSETTLED.code);

        this.setInternalTransactionId(traceId);
        this.setBetId(traceId);
        this.setVendorGameId(gameSession.getVendorGameId());
        this.setVendorPlayerId(gameSession.getVendorPlayerId());
        this.setVendorId(gameSession.getVendorId());
        this.setAgentPlayerId(gameSession.getAgentPlayerId());
        this.setAgentId(gameSession.getAgentId());
        this.setVendorLineId(gameSession.getVendorLineId());
        this.setGameCategoryId(gameSession.getGameCategoryId());
        this.setCurrencyId(gameSession.getCurrencyId());
        this.setGameSessionToken(gameSession.getToken());
        this.setResultType(resultType);
        this.setGameSessionToken(gameSession.getToken());
        this.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
        this.setRawData(rawData);
        this.setIsFreespin(0);
        this.setBalance(BigDecimal.ZERO);

        this.setNewBetAmount(sportBetResultData.getNewBetAmount());
        this.setGameSession(gameSession);

        this.setId(this.generateId());
    }

    public String generateId() {
        return this.getGameSession().getVendorPlayerUsername() + '_' + this.getExternalTransactionId();
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

    public SportUnsettledBetCouchbase(String rawData, BetHistory betHistory, String traceId, Integer resultType, String couchbaseId) {
        super(betHistory);

        this.setStatus(BetStatus.UNSETTLED.code);
        this.setInternalTransactionId(traceId);
        this.setBetId(betHistory.getId());
        this.setVendorGameId(betHistory.getVendorGameId());
        this.setVendorPlayerId(betHistory.getVendorPlayerId());
        this.setVendorId(betHistory.getVendorId());
        this.setAgentPlayerId(betHistory.getAgentPlayerId());
        this.setAgentId(betHistory.getAgentId());
        this.setVendorLineId(betHistory.getVendorLineId());
        this.setGameCategoryId(betHistory.getGameCategoryId());
        this.setCurrencyId(betHistory.getCurrencyId());
        this.setGameSessionToken(betHistory.getGameSessionToken());
        this.setResultType(resultType);
        this.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
        this.setRawData(rawData);
        this.setIsFreespin(0);
        this.setBalance(BigDecimal.ZERO);
        this.setId(couchbaseId);
    }
}
