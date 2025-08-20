package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.engine.wallet.BetTransaction;
import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class BetTxnToBetHistoryMapper {

    /**
     * Maps a single BetTransaction to BetHistory for Kafka processing
     */
    public BetHistory toBetHistory(BetTransaction betTxn, BetResultContext context) {
        BetHistory betHistory = new BetHistory();

        // Map context-level fields (common across all transactions)
        mapContextFields(betHistory, context);

        // Map transaction-specific fields
        mapTransactionFields(betHistory, betTxn, context);

        // Set derived/calculated fields
        setDerivedFields(betHistory, betTxn, context);

        return betHistory;
    }

    /**
     * Maps a list of BetTransactions to BetHistory list
     */
    public List<BetHistory> toBetHistoryList(List<BetTransaction> betTxns, BetResultContext context) {
        if (betTxns == null || betTxns.isEmpty()) {
            return Collections.emptyList();
        }

        return betTxns.stream()
                .map(betTxn -> toBetHistory(betTxn, context))
                .toList();
    }

    /**
     * Maps fields that are common across all transactions from the context
     */
    private void mapContextFields(BetHistory betHistory, BetResultContext context) {
        betHistory.setVendorId(context.getVendorId());
        betHistory.setVendorPlayerId(context.getVendorPlayerId());
        betHistory.setAgentPlayerId(context.getAgentPlayerId());
        betHistory.setAgentId(context.getAgentId());
        betHistory.setCurrencyId(context.getCurrencyId());
        betHistory.setGameCategoryId(context.getGameCategoryId());
        betHistory.setVendorGameId(context.getVendorGameId());
        betHistory.setVendorLineId(context.getVendorLineId());
        betHistory.setGameSessionToken(context.getToken());
        betHistory.setResultTime(context.getResultTime());
    }

    /**
     * Maps fields that are specific to each individual transaction
     */
    private void mapTransactionFields(BetHistory betHistory, BetTransaction betTxn, BetResultContext context) {
        betHistory.setId(UuidUtil.newUuidV7String());
        betHistory.setExternalTransactionId(betTxn.getExternalTransactionId());
        betHistory.setVendorBetId(betTxn.getVendorBetId());
        betHistory.setRoundId(betTxn.getRoundId());
        betHistory.setBetType(betTxn.getBetType());
        betHistory.setBetAmount(betTxn.getBetAmount());
        betHistory.setWinAmount(betTxn.getWinAmount());
        betHistory.setEffectiveTurnover(betTxn.getEffectiveTurnover());
        betHistory.setJackpotAmount(
                Optional.ofNullable(betTxn.getJackpotAmount())
                        .orElse(BigDecimal.ZERO)
        );
        betHistory.setIsFreespin(betTxn.getIsFreeSpin());
        betHistory.setVendorBetTime(context.getVendorBetTime());
        betHistory.setVendorSettleTime(betTxn.getVendorSettleTime());
    }

    /**
     * Sets derived and calculated fields
     */
    private void setDerivedFields(BetHistory betHistory, BetTransaction betTxn, BetResultContext context) {
        betHistory.setWinLoss(calculateWinLoss(betTxn));

        // Set derived business fields
        betHistory.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
        betHistory.setResultType(deriveResultType(betTxn));

        // Set default values
        betHistory.setStatus(1); // Default for new transactions
        betHistory.setResettleNum(0);
    }

    /**
     * Calculates win/loss amount (win amount - bet amount)
     */
    private BigDecimal calculateWinLoss(BetTransaction betTxn) {
        BigDecimal winAmount = Optional.ofNullable(betTxn.getWinAmount()).orElse(BigDecimal.ZERO);
        BigDecimal betAmount = Optional.ofNullable(betTxn.getBetAmount()).orElse(BigDecimal.ZERO);
        return winAmount.subtract(betAmount);
    }

    /**
     * Maps transaction result type to BetHistory result type
     */
    private Integer deriveResultType(BetTransaction betTxn) {
        return BetHistory.retrieveResultType(betTxn.getWinAmount(), betTxn.getJackpotAmount());
    }
}
