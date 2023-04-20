package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.RawGameSession;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("raw")
@Collection("game_session")
public interface RawGameSessionRepository extends CouchbaseRepository<RawGameSession, String> {
    RawGameSession findByAgentIdAndTraceId(Integer agentId, String traceId);

    RawGameSession findByToken(String token);

    RawGameSession findTop1ByVendorPlayerUsernameOrderByCreateTimeDesc(String vendorPlayerUsername);

    RawGameSession findTop1ByVendorPlayerUsernameAndVendorGameCodeOrderByCreateTimeDesc(String vendorPlayerUsername, String vendorGameCode);
}
