package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.GameSession;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    GameSession findByAgentIdAndTraceId(Integer agentId, String traceId);

    GameSession findByToken(String token);

    @Query(value = "SELECT gs FROM GameSession gs WHERE gs.vendorPlayerUsername = :vendorPlayerUsername ORDER BY gs.id DESC")
    GameSession findTop1ByVendorPlayerUsername(@Param("vendorPlayerUsername") String vendorPlayerUsername);

    @Query(value = "SELECT gs FROM GameSession gs WHERE gs.vendorPlayerUsername = :vendorPlayerUsername AND gs.vendorGameCode = :vendorGameCode ORDER BY gs.id DESC")
    GameSession findTop1ByVendorPlayerUsernameAndVendorGameCode(@Param("vendorPlayerUsername") String vendorPlayerUsername, @Param("vendorGameCode") String vendorGameCode);
}
