package com.nextgen.gameaggregator.service.data.producer.transactionhistory;

import com.nextgen.gameaggregator.core.engine.wallet.credit.CreditContext;
import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.GameCategory;
import com.nextgen.gameaggregator.core.entity.Vendor;
import com.nextgen.gameaggregator.core.service.AgentDataService;
import com.nextgen.gameaggregator.core.service.GameCategoryDataService;
import com.nextgen.gameaggregator.core.service.VendorDataService;
import com.nextgen.gameaggregator.entity.couchbase.CreditTxn;
import com.nextgen.gameaggregator.entity.ga.BetHistoryV3;
import com.nextgen.gameaggregator.enums.BetResultType;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.data.model.TxnAmount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Emits one {@link BetHistoryV3} kafka record per credit (settle or refund) in the
 * debit/credit framework. Analogue of the legacy
 * {@link com.nextgen.gameaggregator.service.data.producer.BetHistoryProducer}'s
 * {@code publishBetHistoryForResult} / {@code publishBetHistoryForRollback} for the
 * BET/RESULT family — same kafka topic ({@code BetHistory} via
 * {@link KafkaService#produceBetHistoryV3}).
 *
 * <p>Field-population conventions:
 * <ul>
 *   <li><b>id</b> — uses the credit's own {@code gaTxnId} as the BetHistory record id.
 *       Step 3 follow-up (when upstream debit lookup wires up) may flip this to the
 *       resolved upstream {@code DebitTxn.gaTxnId} for analytics parity with the legacy
 *       BET path where {@code id = betTxn.gaBetId}.</li>
 *   <li><b>status</b> — {@link BetStatus#SETTLED} for settle credits; {@link BetStatus#REFUNDED}
 *       for refunds. Legacy distinguishes {@link BetStatus#CANCELLED} (post-settle rollback,
 *       with {@code negateAmounts}) from {@link BetStatus#REFUNDED} (pre-settle refund) by
 *       inspecting the upstream debit's {@code state}; that branch requires Step 2's
 *       {@code DebitTxn.state} CAS lifecycle and is deferred. Current implementation always
 *       emits {@link BetStatus#REFUNDED} for refunds.</li>
 *   <li><b>amounts</b> — converted from vendor currency via
 *       {@link TxnAmount#of(BigDecimal, BigDecimal)} using {@code context.fromVendorRate}.
 *       Refund payload zeros out win/winLoss/effectiveTurnover/jackpot, mirroring
 *       {@link com.nextgen.gameaggregator.core.engine.operator.wallet.credit.OperatorCreditRequestMapper}'s
 *       refund branch.</li>
 * </ul>
 *
 * <p>Caller responsibility: emit always (success or operator-failure), wrapped in try/catch
 * so a kafka failure cannot mask the operator outcome — matches the
 * {@link WalletTransactionHistoryProducer} pattern in {@code CreditProcessor.finally}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletBetHistoryProducer {
    private final KafkaService kafkaService;
    private final AgentDataService agentDataService;
    private final GameCategoryDataService gameCategoryDataService;
    private final VendorDataService vendorDataService;

    /** Settle or refund — branches internally on {@code context.isRefund}. */
    public void publishBetHistoryForCredit(CreditContext context, CreditTxn credit) {
        Agent agent = agentDataService.get(context.getAgentId());
        Vendor vendor = vendorDataService.get(context.getVendorId());
        GameCategory gameCategory = gameCategoryDataService.get(context.getGameCategoryId());

        BetHistoryV3 history = new BetHistoryV3();

        // Identifiers
        history.setId(credit.getGaTxnId());
        history.setExternalTransactionId(context.getIdempotencyKey());
        history.setVendorBetId(context.getVendorTxnId());
        history.setRoundId(context.getVendorRoundId());

        // Vendor
        history.setVendorId(context.getVendorId());
        history.setVendorCode(vendor.getCode());
        history.setVendorGameId(context.getVendorGameId());
        history.setVendorLineId(context.getVendorLineId());
        history.setVendorPlayerId(context.getVendorPlayerId());
        history.setVendorPlayerUsername(context.getVendorPlayerUsername());

        // Product (legacy parity: empty/zero — not surfaced by the debit/credit world)
        history.setProductId(vendor.getProductId());
        history.setProductCode("");
        history.setProductGameId(0);

        // Agent
        history.setAgentId(context.getAgentId());
        history.setAgentPlayerId(context.getAgentPlayerId());
        history.setAgentPlayerUsername(context.getAgentPlayerUsername());
        history.setHouseId(agent.getHouseId());
        history.setMasterAgentId(agent.getMasterAgentId());

        // GA
        history.setGameCategoryId(context.getGameCategoryId());
        history.setGameCategoryCode(gameCategory.getCode());
        history.setGameCode(context.getGameCode());
        history.setCurrencyId(context.getCurrencyId());
        history.setCurrencyCode(context.getCurrencyCode());
        history.setGameSessionToken(context.getToken());

        // Status / type
        history.setBetType(BetType.NORMAL_BET.code);
        history.setIsFreespin(0);
        history.setOperatorStatus(0);
        history.setResettleNum(0);

        // Times
        long now = System.currentTimeMillis();
        Long vendorBetTime = context.getVendorBetTime() != null
                ? context.getVendorBetTime()
                : context.getVendorTimestamp();
        Long vendorSettleTime = context.getVendorSettleTime() != null
                ? context.getVendorSettleTime()
                : context.getVendorTimestamp();
        history.setVendorBetTime(vendorBetTime);
        history.setVendorSettleTime(vendorSettleTime);
        history.setResultTime(now);

        // Amounts + status
        if (context.isRefund()) {
            populateRefundAmounts(history, context);
            history.setStatus(BetStatus.REFUNDED.code);
            history.setResultType(BetResultType.BET.code);
        } else {
            populateSettleAmounts(history, context);
            history.setStatus(BetStatus.SETTLED.code);
            history.setResultType(getResultType(history.getWinAmount(), history.getJackpotAmount()));
        }

        kafkaService.produceBetHistoryV3(history);

        log.debug("Published wallet bet history id={}, vendorBetId={}, status={}",
                history.getId(), history.getVendorBetId(), history.getStatus());
    }

    private void populateSettleAmounts(BetHistoryV3 history, CreditContext context) {
        BigDecimal rate = context.getFromVendorRate();
        history.setBetAmount(TxnAmount.of(context.getVendorBetAmount(), rate).amount());
        history.setWinAmount(TxnAmount.of(context.getVendorWinAmount(), rate).amount());
        history.setWinLoss(TxnAmount.of(context.resolveVendorWinLoss(), rate).amount());
        history.setEffectiveTurnover(TxnAmount.of(context.resolveVendorEffectiveTurnover(), rate).amount());
        history.setJackpotAmount(TxnAmount.of(context.getVendorJackpotAmount(), rate).amount());
    }

    private void populateRefundAmounts(BetHistoryV3 history, CreditContext context) {
        BigDecimal rate = context.getFromVendorRate();
        // Refund mode: bet = original wager being refunded; vendor doesn't supply a win
        // amount on refund so winAmount = 0.
        BigDecimal refundBet = context.getVendorBetAmount() != null
                ? context.getVendorBetAmount()
                : context.getVendorAmount();
        BigDecimal betAmount = TxnAmount.of(refundBet, rate).amount();
        history.setBetAmount(betAmount);
        history.setWinAmount(BigDecimal.ZERO);
        // Mirror legacy WalletBetCreditProcessor: winLoss = winAmount - betAmount.
        // For refund (winAmount=0, betAmount=refund value), this yields a NEGATIVE
        // winLoss equal to -refundBet — representing money returned to the player.
        // Critical: downstream BI sums winLoss for net-player-PNL; emitting zero here
        // would silently under-report refunds. Operator wire still emits zero per its
        // contract — see OperatorCreditRequestMapper.refund branch.
        history.setWinLoss(betAmount.negate());
        history.setEffectiveTurnover(BigDecimal.ZERO);
        history.setJackpotAmount(BigDecimal.ZERO);
    }

    private static Integer getResultType(BigDecimal win, BigDecimal jackpot) {
        BigDecimal jackpotAmt = Optional.ofNullable(jackpot).orElse(BigDecimal.ZERO);
        BigDecimal winAmt = Optional.ofNullable(win).orElse(BigDecimal.ZERO);
        if (jackpotAmt.signum() > 0) return BetResultType.JACKPOT.code;
        if (winAmt.signum() > 0) return BetResultType.WIN.code;
        return BetResultType.LOSE.code;
    }
}
