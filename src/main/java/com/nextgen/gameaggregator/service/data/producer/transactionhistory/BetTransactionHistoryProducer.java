package com.nextgen.gameaggregator.service.data.producer.transactionhistory;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.GameCategory;
import com.nextgen.gameaggregator.core.entity.Vendor;
import com.nextgen.gameaggregator.core.entity.VendorCurrency;
import com.nextgen.gameaggregator.core.service.AgentDataService;
import com.nextgen.gameaggregator.core.service.GameCategoryDataService;
import com.nextgen.gameaggregator.core.service.VendorCurrencyDataService;
import com.nextgen.gameaggregator.core.service.VendorDataService;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.BetTransactionHistory;
import com.nextgen.gameaggregator.service.KafkaService;
import com.nextgen.gameaggregator.service.data.model.TxnAmount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BetTransactionHistoryProducer {
    private final KafkaService kafkaService;
    private final AgentDataService agentDataService;
    private final GameCategoryDataService gameCategoryDataService;
    private final VendorCurrencyDataService vendorCurrencyService;
    private final VendorDataService vendorDataService;
    private final BetTransactionIntentResolver betResolver;
    private final ResultTransactionIntentResolver resultResolver;
    private final BetAndResultTransactionIntentResolver betAndResultResolver;
    private final RollbackTransactionIntentResolver rollbackResolver;
    private final TransactionHistoryMapper transactionHistoryMapper;

    public void publishTransactionHistoryForBet(BetContext context, GameRound round, GameTransaction txn) {
        publish(new BetContextTransactionHistoryAdapter(context), betResolver.resolve(context, txn), round);
    }

    public void publishTransactionHistoryForResult(BetResultContext context, GameRound round, GameTransaction txn) {
        publish(new BetResultContextTransactionHistoryAdapter(context), resultResolver.resolve(context, round, txn), round);
    }

    /**
     * For Bet and Result, We need to send 1 Bet Transaction and 1 Result Transaction
     */
    public void publishTransactionHistoryForBetAndResult(BetResultContext context, GameRound round, GameTransaction txn) {
        publish(new BetResultContextTransactionHistoryAdapter(context), betAndResultResolver.resolve(context, txn), round);
    }

    /**
     * If Bet Transaction is a BetNResult, we MAY need to send 2 separate Transaction History (Condition: WinAmount > 0)
     */
    public void publishTransactionHistoryForRollback(BetRollbackContext context, GameRound round, GameTransaction betTxn) {
        publish(new BetRollbackContextTransactionHistoryAdapter(context), rollbackResolver.resolve(betTxn), round);
    }


    private void publish(TransactionHistoryContext adapter, List<TransactionIntent> intents, GameRound round) {
        Agent agent = agentDataService.get(round.getAgentMeta().getAgentId());
        Vendor vendor = vendorDataService.get(adapter.vendorId());
        GameCategory gameCategory = gameCategoryDataService.get(adapter.gameCategoryId());
        VendorCurrency vendorCurrency = vendorCurrencyService.getByVendorIdAndCurrencyId(adapter.vendorId(), adapter.currencyId());

        for (TransactionIntent intent : intents) {

            BetTransactionHistory history = transactionHistoryMapper.from(adapter, agent, vendor, gameCategory, intent.type());
            history.setGaBetId(intent.gaBetId());

            TxnAmount txnAmount = TxnAmount.of(intent.amount(), vendorCurrency.getFromVendorRate());

            history.setCurrencyCode(round.getAgentMeta().getCurrency());
            history.setAmount(txnAmount.amount());

            kafkaService.produceBetTransactionHistory(history);

            log.debug("Published txn history id={}, gaBetId={}, type={}",
                history.getExternalTransactionId(),
                history.getGaBetId(),
                history.getTransactionType()
            );
        }
    }
}