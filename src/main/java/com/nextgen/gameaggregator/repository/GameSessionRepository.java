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
    @Query("SELECT gs FROM game_session gs WHERE vendor_player_username = :username")
    GameSession findTop1ByVendorPlayerUsernameOrderByIdDesc(@Param("username") String vendorPlayerUsername);
    @Query("SELECT gs FROM game_session gs WHERE vendor_player_username = :username AND vendor_game_code = :vendorGameCode")
    GameSession findTop1ByVendorPlayerUsernameAndVendorGameCodeOrderByIdDesc(@Param("username") String vendorPlayerUsername, @Param("vendorGameCode") String vendorGameCode);
}
