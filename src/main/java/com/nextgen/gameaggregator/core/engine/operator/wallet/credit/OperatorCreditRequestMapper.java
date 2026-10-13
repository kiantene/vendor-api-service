package com.nextgen.gameaggregator.core.engine.operator.wallet.credit;

import com.nextgen.gameaggregator.core.engine.wallet.credit.CreditContext;
import com.nextgen.gameaggregator.entity.couchbase.CreditTxn;
import com.nextgen.gameaggregator.service.data.model.TxnAmount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Translates a {@link CreditContext} + {@link CreditTxn} into the outbound
 * {@link OperatorCreditRequest} the operator expects. Branches on {@link CreditContext#isRefund()}
 * to populate settle-mode analytics OR zero them out for refund.
 * <p>
 * For settle mode the analytics fields ({@code winAmount}, {@code effectiveTurnover}, etc.) carry
 * the vendor's settlement totals. For refund mode they zero out and {@code amount} carries the
 * refund value — matching the legacy wire produced by
 * {@code WalletBetCreditProcessor.prepareOperatorRequestData(...)}.
 * <p>
 * {@code betId} is set to this credit's own {@code gaTxnId}. Per the operator API spec
 * (verified 2026-06-02): {@code /wallet/bet_debit} has NO {@code betId} field — the operator
 * never receives a betId at debit time, so there is no upstream-debit value for a credit's
 * {@code betId} to cross-link to on the operator side. The field is a required identifier
 * for the credit request itself, not a back-reference. GA-side cross-linking (analytics
 * joins from credit → originating debit) is handled by {@code CreditTxn.referencedDebitGaTxnIds}
 * (audit field; not wired in Slice 1c — Step 3 follow-up).
 */
@Component
@RequiredArgsConstructor
public class OperatorCreditRequestMapper {

    public OperatorCreditRequest toOperatorRequest(CreditContext context, CreditTxn txn) {
        OperatorCreditRequest dto = new OperatorCreditRequest();

        dto.setTraceId(context.getTraceId());
        dto.setTransactionId(txn.getGaTxnId());
        dto.setBetId(txn.getGaTxnId());
        dto.setUsername(context.getAgentPlayerUsername());
        dto.setCurrency(context.getCurrencyCode());
        dto.setToken(context.getToken());
        dto.setGameCode(context.getGameCode());
        dto.setRoundId(context.getVendorRoundId());
        dto.setTimestamp(context.getVendorTimestamp());
        dto.setIsRefund(context.isRefund() ? 1 : 0);

        TxnAmount amount = TxnAmount.of(context.getVendorAmount(), context.getFromVendorRate());
        dto.setAmount(amount.amount());

        if (context.isRefund()) {
            // Refund — wallet movement = refund amount; analytics zeroed.
            // For most vendors, betAmount on refund equals the refund amount (the original wager
            // being returned). If context.vendorBetAmount is null, fall back to the refund amount.
            BigDecimal refundBet = context.getVendorBetAmount() != null ? context.getVendorBetAmount() : context.getVendorAmount();
            TxnAmount refundBetAmount = TxnAmount.of(refundBet, context.getFromVendorRate());
            dto.setBetAmount(refundBetAmount.amount());
            dto.setWinAmount(BigDecimal.ZERO);
            dto.setEffectiveTurnover(BigDecimal.ZERO);
            dto.setWinLoss(BigDecimal.ZERO);
            dto.setJackpotAmount(BigDecimal.ZERO);
            dto.setBetTime(context.getVendorBetTime() != null ? context.getVendorBetTime() : context.getVendorTimestamp());
            dto.setSettledTime(context.getVendorTimestamp());
        } else {
            // Settle — operator wants the full analytics passthrough for reconciliation.
            TxnAmount bet = TxnAmount.of(context.getVendorBetAmount(), context.getFromVendorRate());
            TxnAmount win = TxnAmount.of(context.getVendorWinAmount(), context.getFromVendorRate());
            TxnAmount eff = TxnAmount.of(context.resolveVendorEffectiveTurnover(), context.getFromVendorRate());
            TxnAmount winLoss = TxnAmount.of(context.resolveVendorWinLoss(), context.getFromVendorRate());
            TxnAmount jackpot = TxnAmount.of(context.getVendorJackpotAmount(), context.getFromVendorRate());

            dto.setBetAmount(bet.amount());
            dto.setWinAmount(win.amount());
            dto.setEffectiveTurnover(eff.amount());
            dto.setWinLoss(winLoss.amount());
            dto.setJackpotAmount(jackpot.amount());
            dto.setBetTime(context.getVendorBetTime());
            dto.setSettledTime(context.getVendorSettleTime());
        }

        return dto;
    }
}
