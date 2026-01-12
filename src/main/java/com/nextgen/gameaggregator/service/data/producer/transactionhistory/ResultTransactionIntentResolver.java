package com.nextgen.gameaggregator.service.data.producer.transactionhistory;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.BetTransactionType;
import com.nextgen.gameaggregator.enums.TxnType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ResultTransactionIntentResolver {
    public List<TransactionIntent> resolve(BetResultContext context, GameRound round, GameTransaction txn) {
        return List.of(
            new TransactionIntent(
                BetTransactionType.RESULT,
                context.getWinAmount(),
                resolveGaBetId(round, txn)
            )
        );
    }

    private String resolveGaBetId(GameRound round, GameTransaction txn) {
        Optional<RoundTxn> betTxn = round.getTransactions()
                                            .stream()
                                            .filter(t -> t.getType() == TxnType.BET)
                                            .findFirst();

        if (betTxn.isPresent()) {
            return betTxn.get().getGaBetId();
        } else {
            return txn.getGaBetId();
        }
    }
}
