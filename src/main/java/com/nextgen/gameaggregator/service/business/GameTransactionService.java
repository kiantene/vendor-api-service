package com.nextgen.gameaggregator.service.business;

import com.couchbase.client.core.error.DocumentExistsException;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.nextgen.gameaggregator.core.exception.BetNotFoundException;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.service.data.GameTransactionDataService;
import com.nextgen.gameaggregator.service.data.model.TxnDelta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameTransactionService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneOffset.UTC);

    private final GameTransactionDataService txnDataService;
    private final GameRoundService gameRoundService;

    public Optional<GameTransaction> get(GameTransaction txn) {
        GameTransaction doc = txnDataService.findById(txn.getId());

        return doc != null ? Optional.of(doc) : Optional.empty();
    }

    public Optional<GameTransaction> get(String id) {
        GameTransaction doc = txnDataService.findById(id);

        return doc != null ? Optional.of(doc) : Optional.empty();
    }

    public GameTransaction getOrThrow(String id) {
        return get(id).orElseThrow(() -> new BetNotFoundException("GameTransaction not found: " + id));
    }

    public GameTransaction save(GameTransaction txn) {
        if (TxnStatus.SENT == txn.getStatus()) {
            txn.setSentAt(getNow());
        }
        txnDataService.insert(txn);
        return txn;
    }

    public GameRound markSent(GameTransaction txn, AgentMeta agentMeta) {
        txn.setStatus(TxnStatus.SENT);
        txn.setSentAt(getNow());
        txnDataService.update(txn);

        /**
         * Create an alias copy of the same txn but using vendorBetId as primary key (docId)
         * This alias copy is used for rollback using vendorBetId and will expire in 3 days
         */
        if (shouldCreateAliasTxn(txn)) {
            createAliasTxn(txn);
        }

        return gameRoundService.save(txn, agentMeta);
    }

    public void markPending(GameTransaction txn) {
        // TODO: update status
    }

    public void markSuccess(GameRound round, GameTransaction txn, BigDecimal balance) {
        markSuccess(round, txn, balance, false);
    }

    public GameRound markSuccess(GameRound round, GameTransaction txn, BigDecimal balance, Boolean isEnded) {
        // Defense-in-depth: idx must be assigned before finalizing (TxnDelta.idx is a
        // primitive int). Fail fast with context instead of an opaque NPE on unboxing.
        if (txn.getIdx() == null) {
            throw new IllegalStateException(
                    "markSuccess called with unassigned idx for txn " + txn.getId()
                            + " — transaction was not appended to its round");
        }
        txn.setStatus(TxnStatus.SUCCESS);
        txn.setDoneAt(getNow());
        txnDataService.updateStatus(txn, balance, TxnStatus.SUCCESS);

        if (shouldCreateAliasTxn(txn)) {
            GameTransaction betTxn = txn.copy();
            betTxn.setType(TxnType.BET);
            betTxn.setTransactionId(txn.getVendorBetId());
            betTxn.setState(txn.getState());
            txnDataService.updateStatus(betTxn, balance, TxnStatus.SUCCESS);
        }

        TxnDelta delta = TxnDelta.finalizeSuccess(
                txn.getRoundDocId(),
                txn.getIdx(),
                txn.getType(),
                txn.getGaBetId(),
                balance,
                txn.getBetAmount(),
                txn.getWinAmount(),
                txn.getJackpotAmount(),
                txn.getEffectiveTurnover(),
                txn.getDoneAt(),
                isSettled(txn, isEnded),
                Optional.ofNullable(isEnded).orElse(false)
        );

        return gameRoundService.applyTxnDelta(delta);
    }

    private boolean isSettled(GameTransaction txn, Boolean isEnded) {
        // If isEnded is not NULL and false, we should not update RoundState for gameRound
        return !Boolean.FALSE.equals(isEnded)
                && GameRoundState.SETTLED == txn.getState();
    }

    public void markError(GameTransaction txn, RuntimeException ex) {
        if (txn == null) return;

        String exName = getMeaningfulExceptionName(ex);
        txn.setException(exName);
        txnDataService.updateStatus(txn, null, TxnStatus.ERROR);

        if (shouldCreateAliasTxn(txn)) {
            try {
                GameTransaction betTxn = txn.copy();
                betTxn.setType(TxnType.BET);
                betTxn.setTransactionId(txn.getVendorBetId());
                txnDataService.updateStatus(betTxn, null, TxnStatus.ERROR);
            } catch (DocumentNotFoundException e) {
                log.warn("Alias BET " + txn.getVendorBetId() + " Txn has not been created yet");
            }
        }

        Map<String, Object> updates = Map.of(
                "status", TxnStatus.ERROR.name(),
                "exception", exName,
                "doneAt", getNow()
        );

        if (txn.getIdx() != null) {
            gameRoundService.updateRoundTxn(txn.getRoundDocId(), txn.getIdx(), updates);
        } else {
            log.error("idx is null for txn.docId: " + txn.getId());
        }
    }

    public void markRollback(GameRound round, GameTransaction rollbackTxn, BigDecimal balance) {
        rollbackTxn.setState(GameRoundState.COMPLETED);
        txnDataService.updateStatus(rollbackTxn, balance, TxnStatus.SUCCESS);
        gameRoundService.updateRoundTxn(rollbackTxn, GameRoundState.COMPLETED);

        /**
         * to revisit this logic: to consider removing state at GameRound level and
         * change GameRoundState to TxnState
         */
//        gameRoundService.updateRoundState(round.getId(), GameRoundState.REFUNDED);

        // TODO: to deduct amounts from GameRound after rollback
//        TxnDelta delta = TxnDelta.finalizeSuccess(
//                betTxn.getRoundDocId(),
//                betTxn.getIdx(),
//                betTxn.getType(),
//                betTxn.getGaBetId(),
//                balance,
//                betTxn.getBetAmount().negate(),
//                betTxn.getWinAmount().negate(),
//                betTxn.getJackpotAmount().negate(),
//                betTxn.getDoneAt(),
//                GameRoundState.SETTLED == betTxn.getState(),
//                true
//        );
//
//        gameRoundService.applyTxnDelta(delta);
    }

    public void markRefunded(String docId) {
        get(docId).ifPresent(this::markRefunded);
    }

    public void markRefunded(GameTransaction txn) {

        txnDataService.updateToRefunded(txn.getId());
        gameRoundService.updateRoundTxn(txn, GameRoundState.REFUNDED);
    }

    public void markSettled(String txnId, long settledTime) {
        txnDataService.updateToSettled(txnId, settledTime);
    }

    public void deleteById(String id) {
        txnDataService.deleteById(id);
    }

    private String getNow() {
        return TIME_FORMATTER.format(Instant.now());
    }

    private String getMeaningfulExceptionName(RuntimeException ex) {
        Throwable current = ex;

        // Keep unwrapping while we have generic RuntimeException with causes
        while (current.getClass() == RuntimeException.class && current.getCause() != null) {
            current = current.getCause();
        }

        return current.getClass().getSimpleName();
    }

    private void createAliasTxn(GameTransaction txn) {
        int ttl = 3;
        GameTransaction betTxn = txn.copy();
        betTxn.setType(TxnType.BET); // alias txn will always be type BET so that getRollbackId can find
        betTxn.setTransactionId(txn.getVendorBetId());
        try {
            txnDataService.insertWithTTL(betTxn, Duration.ofDays(ttl));
        } catch (DocumentExistsException ex) {
            // don't throw error even if document already exists
            log.warn("GameTransaction (" + betTxn.getId() + ") already exists");
        }
    }

    private boolean shouldCreateAliasTxn(GameTransaction txn) {
        // if vendor bet id is different from transaction id
        boolean isVendorBetIdDifferent = !txn.getTransactionId().equals(txn.getVendorBetId());

        //As Rollback searches by <classname>::BET::<vendorbetid>, we have to create an alias txn for BetNResult
        //scenario OR Bet transaction with different vendorbetid and txnid
        return txn.isBetNResult() || (txn.isBet() && isVendorBetIdDifferent);
    }
}
