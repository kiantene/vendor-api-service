package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.WalletTransaction;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Scope("raw")
@Collection("wallet_transaction")
public interface WalletTransactionRepository extends CouchbaseRepository<WalletTransaction, String> {
    List<WalletTransaction> findByRoundIdAndVendorPlayerUsername(String roundId, String vendorPlayerUsername);

    WalletTransaction findByVendorIdAndExternalTransactionId(Integer vendorId, String externalTransactionId);
}
