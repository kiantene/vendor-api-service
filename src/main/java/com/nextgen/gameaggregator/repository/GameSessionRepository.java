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

    @Query(value = "SELECT gs FROM game_session gs WHERE gs.vendor_player_username = :vendorPlayerUsername ORDER BY ds.id DESC LIMIT 1", nativeQuery = true)
    GameSession findTop1ByVendorPlayerUsernameOrderByIdDesc(@Param("vendorPlayerUsername") String vendorPlayerUsername);

    @Query(value = "SELECT gs FROM game_session gs WHERE gs.vendor_player_username = :vendorPlayerUsername AND gs.vendor_game_code = :vendorGameCode ORDER BY ds.id DESC LIMIT 1", nativeQuery = true)
    GameSession findTop1ByVendorPlayerUsernameAndVendorGameCodeOrderByIdDesc(@Param("vendorPlayerUsername") String vendorPlayerUsername, @Param("vendorGameCode") String vendorGameCode);
}
