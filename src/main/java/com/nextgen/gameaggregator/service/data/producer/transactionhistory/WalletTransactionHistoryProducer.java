package com.nextgen.gameaggregator.service.data.producer.transactionhistory;

import com.nextgen.gameaggregator.core.engine.wallet.credit.CreditContext;
import com.nextgen.gameaggregator.core.engine.wallet.debit.DebitContext;
import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.GameCategory;
import com.nextgen.gameaggregator.core.entity.Vendor;
import com.nextgen.gameaggregator.core.service.AgentDataService;
import com.nextgen.gameaggregator.core.service.GameCategoryDataService;
import com.nextgen.gameaggregator.core.service.VendorDataService;
import com.nextgen.gameaggregator.entity.couchbase.CreditTxn;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.ga.BetTransactionHistory;
import com.nextgen.gameaggregator.enums.BetTransactionType;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.data.model.TxnAmount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Emits one {@link BetTransactionHistory} kafka record per wallet movement in the
 * debit/credit framework. Analogue of {@code BetTransactionHistoryProducer} for the
 * BET/RESULT family — same topic ({@code BetTransactionHistory} via
 * {@link KafkaService#produceBetTransactionHistory}), different
 * {@link BetTransactionType} discriminator ({@code DEBIT} / {@code CREDIT}).
 *
 * <p>Each method emits exactly one record — no fan-out, no {@code TransactionIntent}
 * machinery. Debit/credit txns are atomic per call from the vendor's perspective.
 *
 * <p>Refund vs settle is encoded on the {@code CreditTxn.state} field that's already
 * persisted; this producer doesn't disambiguate them at the kafka layer.
 *
 * <p>Caller is responsible for gating on {@code vendorConfigService.isTransactionHistoryEnabled(...)} —
 * mirrors the existing pattern in {@code BetProcessor.onAfterSendBet}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletTransactionHistoryProducer {
    private final KafkaService kafkaService;
    private final AgentDataService agentDataService;
    private final GameCategoryDataService gameCategoryDataService;
    private final VendorDataService vendorDataService;
    private final TransactionHistoryMapper transactionHistoryMapper;

    /** Debit wallet movement. */
    public void publishTransactionHistoryForDebit(DebitContext context, DebitTxn debit) {
        publish(
                new DebitContextTransactionHistoryAdapter(context),
                BetTransactionType.DEBIT,
                context.getVendorAmount(),
                debit.getGaTxnId(),
                context.getCurrencyCode(),
                context.getFromVendorRate()
        );
    }

    /** Settle or refund — both are CREDIT wallet movements. */
    public void publishTransactionHistoryForCredit(CreditContext context, CreditTxn credit) {
        publish(
                new CreditContextTransactionHistoryAdapter(context),
                BetTransactionType.CREDIT,
                context.getVendorAmount(),
                credit.getGaTxnId(),
                context.getCurrencyCode(),
                context.getFromVendorRate()
        );
    }

    private void publish(TransactionHistoryContext adapter,
                         BetTransactionType type,
                         BigDecimal vendorAmount,
                         String gaTxnId,
                         String agentCurrency,
                         BigDecimal fromVendorRate) {
        Agent agent = agentDataService.get(adapter.agentId());
        Vendor vendor = vendorDataService.get(adapter.vendorId());
        GameCategory gameCategory = gameCategoryDataService.get(adapter.gameCategoryId());

        BetTransactionHistory history = transactionHistoryMapper.from(adapter, agent, vendor, gameCategory, type);
        history.setGaBetId(gaTxnId);

        TxnAmount txnAmount = TxnAmount.of(vendorAmount, fromVendorRate);
        history.setCurrencyCode(agentCurrency);
        history.setAmount(txnAmount.amount());

        kafkaService.produceBetTransactionHistory(history);

        log.debug("Published wallet txn history id={}, gaTxnId={}, type={}",
                history.getExternalTransactionId(),
                history.getGaBetId(),
                history.getTransactionType()
        );
    }
}
