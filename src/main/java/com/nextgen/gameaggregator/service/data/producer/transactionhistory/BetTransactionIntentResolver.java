package com.nextgen.gameaggregator.service.data.producer.transactionhistory;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.BetTransactionType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BetTransactionIntentResolver {

    public List<TransactionIntent> resolve(BetContext ctx, GameTransaction txn) {
        return List.of(
            new TransactionIntent(
                BetTransactionType.BET,
                ctx.getBetAmount(),
                txn.getGaBetId()
            )
        );
    }
}
