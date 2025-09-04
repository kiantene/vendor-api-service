package com.nextgen.gameaggregator.service.data.couchbase;

import com.couchbase.client.core.error.CasMismatchException;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.repository.couchbase.GameRoundRepository;
import com.nextgen.gameaggregator.service.data.GameRoundDataService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
public class CouchbaseGameRoundDataService implements GameRoundDataService {
    private final GameRoundRepository repo;

    public CouchbaseGameRoundDataService(GameRoundRepository repo) {
        this.repo = repo;
    }

    @Override
    public KvDoc<GameRound> findById(String id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    public void insert(GameRound round) {
        repo.insert(round);
    }

    @Override
    public void appendTxn(String docId, RoundTxn roundTxn, BigDecimal newBetAmount, long cas) {
        runWithCasRetry(() -> repo.appendTxn(docId, roundTxn, newBetAmount, BigDecimal.ZERO, cas));
    }

    @Override
    public void setTxnStatus(String docId, int idx, TxnStatus status, boolean settle, Duration ttlIfSettled) {
        runWithCasRetry(() -> repo.updateTxnStatus(docId, idx, status, settle, ttlIfSettled));
    }

    @Override
    public void setRoundState(String docId, GameRoundState state) {
        repo.updateRoundState(docId, state);
    }

    private static void runWithCasRetry(Runnable op) {
        int attempts = 0;
        while (true) {
            try {
                op.run();
                return;
            } catch (CasMismatchException e) {
                if (++attempts >= 3) throw e;
            }
        }
    }
}
