package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Scope("raw")
@Collection("game_sessions")
public interface RawGameSessionRepository extends CouchbaseRepository<GameSession, String> {
    GameSession findByAgentIdAndTraceId(Integer agentId, String traceId);

    GameSession findByToken(String token);

    GameSession findByVendorToken(String vendorToken);

    GameSession findTop1ByVendorPlayerUsernameOrderByCreateTimeDesc(String vendorPlayerUsername);

    GameSession findTop1ByVendorPlayerUsernameAndVendorGameCodeOrderByCreateTimeDesc(String vendorPlayerUsername, String vendorGameCode);

    List<GameSession> findByAgentPlayerUsernameAndStatus(String userName, Integer status);

    List<GameSession> findByVendorPlayerUsername(String username);

    List<GameSession> findByVendorPlayerUsernameAndVendorGameCode(String username, String vendorGameCode);
}
