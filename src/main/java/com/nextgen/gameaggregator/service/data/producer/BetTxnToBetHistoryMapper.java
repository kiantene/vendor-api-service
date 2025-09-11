package com.nextgen.gameaggregator.service.data.producer;

import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.engine.wallet.BetTransaction;
import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class BetTxnToBetHistoryMapper {

    /**
     * Maps a single BetTransaction to BetHistory for Kafka processing
     */
    public BetHistory mapValues(BetHistory betHistory, BetTransaction betTxn) {
        // Map transaction-specific fields
        mapTransactionFields(betHistory, betTxn);

        // Set derived/calculated fields
        setDerivedFields(betHistory, betTxn);

        return betHistory;
    }

    /**
     * Maps fields that are specific to each individual transaction
     */
    private void mapTransactionFields(BetHistory betHistory, BetTransaction betTxn) {
        betHistory.setId(UuidUtil.newUuidV7String());
        betHistory.setExternalTransactionId(betTxn.getExternalTransactionId());
        betHistory.setVendorBetId(betTxn.getVendorBetId());
        betHistory.setRoundId(betTxn.getRoundId());
        betHistory.setBetType(betTxn.getBetType());
        betHistory.setBetAmount(betTxn.getBetAmount());
        betHistory.setWinAmount(betTxn.getWinAmount());
        betHistory.setEffectiveTurnover(betTxn.getEffectiveTurnover());
        betHistory.setJackpotAmount(
                Optional.ofNullable(betTxn.getJackpotAmount()).orElse(BigDecimal.ZERO)
        );
        betHistory.setIsFreespin(betTxn.getIsFreeSpin());
        betHistory.setVendorSettleTime(betTxn.getVendorSettleTime());
    }

    /**
     * Sets derived and calculated fields
     */
    private void setDerivedFields(BetHistory betHistory, BetTransaction betTxn) {
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
