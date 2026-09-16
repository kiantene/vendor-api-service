package com.nextgen.gameaggregator.vendor.ezugi.api.v2.result;

import com.nextgen.gameaggregator.core.engine.wallet.BetTransaction;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<BetResultRequest> {

    private final GameTransactionService gameTransactionService;

    public BetResultRequestMapper(GameTransactionService gameTransactionService) {
        this.gameTransactionService = gameTransactionService;
    }

    @Override
    public BetResultContext toInternal(BetResultRequest request) {
        BigDecimal betAmount = (request.getGameDataString() != null) ? request.getGameDataString().getBetAmount() : BigDecimal.ZERO;
        BigDecimal winAmount = request.getCreditAmount();
        BigDecimal winloss = winAmount.subtract(betAmount);

        // Default to the updated vendorRoundId mapping logic
        String resolvedRoundId = String.valueOf(request.getRoundId());

        // =========================================================================================================
        // TODO / DEBT: REMOVE AFTER DEPLOYMENT IS STABLE
        // Backward compatibility check for in-flight transactions spanning across deployments:
        // If the debit record exists and was created under legacy mapping (where roundId was mapped to debitTransactionId),
        // preserve debitTransactionId as the resolved roundId to prevent "Debit transaction ID not found" errors during credit.
        // Clean-up target: Remove DB call, docId creation, gameTransactionService dependency, and this entire if-block.
        // =========================================================================================================
        String docId = GameTransaction.createDocId(EndPoints.VENDOR, TxnType.BET, request.getDebitTransactionId());
        Optional<GameTransaction> txnOpt = gameTransactionService.get(docId);

        if (txnOpt.isPresent() && request.getDebitTransactionId().equals(txnOpt.get().getRoundId())) {
            resolvedRoundId = request.getDebitTransactionId();
        }
        // =========================================================================================================

        BetResultContext context = BetResultContext.builder()
                .idempotencyKey(request.getTransactionId())
                .vendorPlayerUsername(request.getUid())
                .vendorBetId(request.getDebitTransactionId())
                .roundId(resolvedRoundId)
                .vendorGameCode(String.valueOf(request.getTableId()))
                .vendorCurrency(request.getCurrency())
                .betAmount(BigDecimal.ZERO)
                .winAmount(winAmount)
                .winloss(winloss)
                .effectiveTurnover(betAmount)
                .vendorSettleTime(request.getTimestamp())
                .vendorSessionToken(request.getToken())
                .roundEnded(request.isEndRound())
                .build();

        List<BetTransaction> betTransactions = BetTransactionMapper.mapToBetTransactions(request, resolvedRoundId);
        context.setBetTransactions(betTransactions);
        return context;
    }
}
