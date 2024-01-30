package com.nextgen.gameaggregator.repository.ga.writer;


import com.nextgen.gameaggregator.entity.ga.RawTransferHistory;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("raw")
@TypeAlias("transfer_histories")
@Collection("transfer_histories")
public interface RawTransferHistoryRepository extends CouchbaseRepository<RawTransferHistory, String> {

    RawTransferHistory findByReferenceIdAndAgentId(String referenceId, Integer agentId);
}
