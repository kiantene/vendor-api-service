package com.nextgen.gameaggregator.service.data.producer.transactionhistory;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.BetTransactionType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BetAndResultTransactionIntentResolver {

    public List<TransactionIntent> resolve(BetResultContext ctx, GameTransaction txn) {
        return List.of(
            new TransactionIntent(
                    BetTransactionType.BET,
                    ctx.getBetAmount(),
                    txn.getGaBetId()
            ),
            new TransactionIntent(
                BetTransactionType.RESULT,
                ctx.getWinAmount(),
                txn.getGaBetId()
            )
        );
    }
}
