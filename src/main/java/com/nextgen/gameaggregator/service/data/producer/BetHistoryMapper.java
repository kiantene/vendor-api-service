package com.nextgen.gameaggregator.service.data.producer;

import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.GameCategory;
import com.nextgen.gameaggregator.core.entity.Vendor;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.ga.BetHistoryV3;
import com.nextgen.gameaggregator.enums.BetResultType;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.service.data.model.TxnAmounts;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class BetHistoryMapper {

    public BetHistoryV3 initialise(BetHistoryContext context, String betId, String externalTransactionId) {
        BetHistoryV3 betHistory = new BetHistoryV3();

        betHistory.setId(betId);
        betHistory.setExternalTransactionId(externalTransactionId);
        betHistory.setVendorGameId(context.getVendorGameId());
        betHistory.setVendorPlayerId(context.getVendorPlayerId());
        betHistory.setVendorId(context.getVendorId());
        betHistory.setVendorLineId(context.getVendorLineId());
        betHistory.setAgentPlayerId(context.getAgentPlayerId());
        betHistory.setCurrencyId(context.getCurrencyId());
        betHistory.setOperatorStatus(0);
        betHistory.setResettleNum(0);

        betHistory.setBetType(BetType.NORMAL_BET.code);
        betHistory.setStatus(BetStatus.SETTLED.code);

        betHistory.setResultTime(context.getResultTime());

        return betHistory;
    }

    public void mapReferenceFields(BetHistoryV3 betHistory,
                                   Agent agent,
                                   Vendor vendor,
                                   GameCategory gameCategory) {

        betHistory.setProductId(vendor.getProductId());
        betHistory.setProductCode("");
        betHistory.setProductGameId(0);
        betHistory.setVendorCode(vendor.getCode());
        betHistory.setHouseId(agent.getHouseId());
        betHistory.setMasterAgentId(agent.getMasterAgentId());
        betHistory.setAgentId(agent.getId());
        betHistory.setGameCategoryId(gameCategory.getId());
        betHistory.setGameCategoryCode(gameCategory.getCode());
    }

    public void mapTransactionFields(BetHistoryV3 betHistory,
                                     GameRound round,
                                     TxnAmounts txnAmounts,
                                     String vendorBetId,
                                     Long vendorBetTime,
                                     Long vendorSettleTime) {

        BigDecimal bet      = txnAmounts.getBet();
        BigDecimal win      = txnAmounts.getWin();
        BigDecimal winLoss  = txnAmounts.getWinLoss();
        BigDecimal turnover = txnAmounts.getTurnover();
        BigDecimal jackpot  = txnAmounts.getJackpot();

        betHistory.setGameSessionToken(round.getAgentMeta().getSession());
        betHistory.setRoundId(round.getRoundId());
        betHistory.setVendorPlayerUsername(round.getUsername());
        betHistory.setVendorBetId(vendorBetId);
        betHistory.setBetAmount(bet);
        betHistory.setWinAmount(win);
        betHistory.setWinLoss(winLoss);
        betHistory.setEffectiveTurnover(turnover);
        betHistory.setJackpotAmount(jackpot);
        betHistory.setIsFreespin(0);
        betHistory.setVendorBetTime(vendorBetTime);

        /**
         * if bet result, settle time follows settlement time from vendor's request
         * if refund bet, settle time follows the rollback request time (rollback unsettled bet)
         * if cancel bet, settle time follows the original settled bet's settled time (rollback settled bet)
         */
        betHistory.setVendorSettleTime(vendorSettleTime);

        // Operator facing fields
        betHistory.setCurrencyCode(round.getAgentMeta().getCurrency());
        betHistory.setGameCode(round.getAgentMeta().getGameCode());
        betHistory.setAgentPlayerUsername(round.getAgentMeta().getUsername());

        betHistory.setResultType(getResultType(win, jackpot));
    }

//    public void mapTransactionFields(BetHistoryV3 betHistory,
//                                     GameRound round,
//                                     GameTransaction txn,
//                                     BigDecimal fromVendorRate,
//                                     boolean fromRound) {
//
//        BigDecimal bet      = convertIfNotZero(fromRound ? round.getBetAmount() : txn.getBetAmount(), fromVendorRate);
//        BigDecimal win      = convertIfNotZero(fromRound ? round.getWinAmount() : txn.getWinAmount(), fromVendorRate);
//        BigDecimal winLoss  = convertIfNotZero(win.subtract(bet), fromVendorRate);
//        BigDecimal turnover = bet;
//        BigDecimal jackpot  = convertIfNotZero(fromRound ? round.getJackpotAmount() : txn.getJackpotAmount(), fromVendorRate);
//
//        betHistory.setGameSessionToken(round.getAgentMeta().getSession());
//        betHistory.setRoundId(round.getRoundId());
//        betHistory.setVendorPlayerUsername(round.getUsername());
//        betHistory.setVendorBetId(txn.getVendorBetId());
//        betHistory.setBetAmount(bet);
//        betHistory.setWinAmount(win);
//        betHistory.setWinLoss(winLoss);
//        betHistory.setEffectiveTurnover(turnover);
//        betHistory.setJackpotAmount(jackpot);
//        betHistory.setIsFreespin(0);
//        betHistory.setVendorBetTime(txn.getBetTime());
//
//        /**
//         * if bet result, settle time follows settlement time from vendor's request
//         * if refund bet, settle time follows the rollback request time (rollback unsettled bet)
//         * if cancel bet, settle time follows the original settled bet's settled time (rollback settled bet)
//         */
//        betHistory.setVendorSettleTime(txn.getSettleTime());
//
//        // Operator facing fields
//        betHistory.setCurrencyCode(round.getAgentMeta().getCurrency());
//        betHistory.setGameCode(round.getAgentMeta().getGameCode());
//        betHistory.setAgentPlayerUsername(round.getAgentMeta().getUsername());
//
//        betHistory.setResultType(getResultType(win, jackpot));
//    }

    public void negateAmounts(BetHistoryV3 betHistory) {
        betHistory.setBetAmount(betHistory.getBetAmount().negate());
        betHistory.setWinAmount(betHistory.getWinAmount().negate());
        betHistory.setWinLoss(betHistory.getWinLoss().negate());
        betHistory.setEffectiveTurnover(betHistory.getEffectiveTurnover().negate());
        betHistory.setJackpotAmount(betHistory.getJackpotAmount().negate());
    }

//    private BigDecimal convertIfNotZero(BigDecimal value, BigDecimal conversionRate) {
//        if (value == null || value.signum() == 0) return BigDecimal.ZERO;
//
//        return new BigDecimal(value.multiply(conversionRate).stripTrailingZeros().toPlainString());
//    }

    private Integer getResultType(BigDecimal win, BigDecimal jackpot) {
        BigDecimal jackpotAmt = Optional.ofNullable(jackpot).orElse(BigDecimal.ZERO);
        BigDecimal winAmt = Optional.ofNullable(win).orElse(BigDecimal.ZERO);

        if (jackpotAmt.signum() > 0) {
            return BetResultType.JACKPOT.code;
        }

        if (winAmt.signum() > 0) {
            return BetResultType.WIN.code;
        }

        return BetResultType.LOSE.code;
    }
}
