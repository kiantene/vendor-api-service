package com.nextgen.gameaggregator.sport.entity;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.entity.ga.BetInformation;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
@Document
@Scope("raw")
@Collection("sport_unsettled_bet")
@Data
@NoArgsConstructor
public class SportUnsettledBet extends BetInformation {
    private BigDecimal newBetAmount;
    private String vendorPlayerUsername;
    private Integer isConfirmBet;
    private Integer isUnsettledBet;
    private Integer unsettledResettleNum = 0;
    private String masterSportUnsettleBetId;

    public SportUnsettledBet(GameSession gameSession, String rawData, SportBetResultData sportBetResultData, String traceId, Integer resultType) {
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
        this.setResettleNum(0);
        this.setIsConfirmBet(0);
        this.setIsUnsettledBet(0);

        //new betType
        this.setBetType(sportBetResultData.getBetType());

        this.setNewBetAmount(sportBetResultData.getNewBetAmount());

        this.setId(this.generateId());
    }

    public SportUnsettledBet(String rawData, BetHistory betHistory, String traceId, Integer resultType, String couchbaseId) {
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

    public SportUnsettledBet(WalletRequest walletRequest) {
        this.setBetId(walletRequest.getBetId());
        this.setInternalTransactionId(walletRequest.getTraceId());
        this.setExternalTransactionId(walletRequest.getExternalTransactionId());
        this.setVendorBetId(walletRequest.getVendorBetId());
        this.setRoundId(walletRequest.getRoundId());
        this.setVendorGameId(walletRequest.getVendorGameId());
        this.setVendorPlayerId(walletRequest.getVendorPlayerId());
        this.setVendorId(walletRequest.getVendorId());
        this.setVendorLineId(walletRequest.getVendorLineId());
        this.setAgentPlayerId(walletRequest.getAgentPlayerId());
        this.setAgentId(walletRequest.getAgentId());
        this.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
        this.setCurrencyId(walletRequest.getCurrencyId());
        this.setBetAmount(Objects.requireNonNullElse(walletRequest.getBetAmount(), BigDecimal.ZERO));
        this.setWinAmount(Objects.requireNonNullElse(walletRequest.getWinAmount(), BigDecimal.ZERO));
        this.setJackpotAmount(Objects.requireNonNullElse(walletRequest.getJackpotAmount(), BigDecimal.ZERO));
        this.setWinLoss(Objects.requireNonNullElse(walletRequest.getWinLoss(), BigDecimal.ZERO));
        this.setEffectiveTurnover(Objects.requireNonNullElse(walletRequest.getEffectiveTurnover(), BigDecimal.ZERO));
        this.setResultType(walletRequest.getResultType());
        this.setIsFreespin(0);
        this.setRawData(walletRequest.getRequestBody());
        this.setResettleNum(0);
        this.setStatus(BetStatus.UNSETTLED.code);
        this.setGameSessionToken(walletRequest.getToken());
        this.setGameCategoryId(walletRequest.getGameCategoryId());
        this.setVendorBetTime(walletRequest.getVendorBetTime());
        this.setVendorSettleTime(walletRequest.getVendorSettleTime());
        this.setBalance(BigDecimal.ZERO);
        this.setBetType(walletRequest.getBetType());
        this.setNewBetAmount(walletRequest.getNewBetAmount());
        this.setVendorPlayerUsername(walletRequest.getVendorPlayerUsername());
        this.setIsConfirmBet(0);
        this.setIsUnsettledBet(0);

        this.setId(this.generateId());

    }

    public String generateId() {
        return this.getVendorPlayerUsername() + '_' + this.getVendorBetId();
    }

    public BetHistory toBetHistory(Integer betStatus, Integer resultType) {
        BetHistory betHistory = new BetHistory();
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(this, betHistory);

        betHistory.setId(this.getBetId());
        betHistory.setStatus(betStatus);
        betHistory.setResultType(resultType);

        // if new bet amount Exists, Bet History stored new bet amount
        Optional.ofNullable(this.getNewBetAmount()).ifPresent(betHistory::setBetAmount);

        return betHistory;
    }
}
