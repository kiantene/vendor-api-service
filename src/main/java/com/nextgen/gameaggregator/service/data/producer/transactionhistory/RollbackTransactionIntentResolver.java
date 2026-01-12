package com.nextgen.gameaggregator.service.data.producer.transactionhistory;

import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.BetTransactionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RollbackTransactionIntentResolver {
    public List<TransactionIntent> resolve(
            GameTransaction betTxn
    ) {
        List<TransactionIntent> intents = new ArrayList<>();

        if (betTxn.isBet() || betTxn.isBetNResult()) {
            intents.add(new TransactionIntent(
                    BetTransactionType.ROLLBACK_BET,
                    betTxn.getBetAmount().negate(),
                    betTxn.getGaBetId()
            ));
        }

        if (betTxn.isResult() || (betTxn.isBetNResult() && betTxn.getWinAmount().signum() > 0)) {
            intents.add(new TransactionIntent(
                    BetTransactionType.ROLLBACK_RESULT,
                    betTxn.getWinAmount().negate(),
                    betTxn.getGaBetId()
            ));
        }

        return intents;
    }
}
