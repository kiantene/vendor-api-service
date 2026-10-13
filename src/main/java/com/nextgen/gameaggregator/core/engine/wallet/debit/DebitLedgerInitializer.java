package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundLedger;
import com.nextgen.gameaggregator.service.data.WalletRoundLedgerDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Ensures a {@link WalletRoundLedger} exists for the current round. Called by the
 * debit pipeline on every leg — {@code insertOrGet} is idempotent so subsequent
 * debits in the same round just return the existing ledger.
 *
 * <p>Returns the persisted {@link KvDoc} for downstream CAS-aware
 * {@code appendTxnRef} use.
 */
@Service
@RequiredArgsConstructor
public class DebitLedgerInitializer {

    private final WalletRoundLedgerDataService walletRoundLedgerDataService;

    public KvDoc<WalletRoundLedger> ensure(DebitContext context, long now) {
        WalletRoundLedger ledger = WalletRoundLedger.of(
                context.getVendorClassName(),
                context.getVendorPlayerUsername(),
                context.getVendorRoundId(),
                context.getVendorId(),
                // WalletRoundLedger.of takes vendorGameCode here — pass the vendor-facing
                // code so the ledger agrees with its own transactions.
                context.getVendorGameCode(),
                context.getCurrencyCode(),
                now);
        return walletRoundLedgerDataService.insertOrGet(ledger)
                .orElseGet(() -> walletRoundLedgerDataService.findById(ledger.getId()));
    }
}
