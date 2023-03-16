package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    GameSession findByAgentIdAndTraceId(Integer agentId, String traceId);
    GameSession findByToken(String token);
    GameSession findTop1ByVendorPlayerUsernameOrderByIdDesc(String vendorPlayerUsername);
    GameSession findTop1ByVendorPlayerUsernameAndVendorGameCodeOrderByIdDesc(String vendorPlayerUsername, String vendorGameCode);
}
