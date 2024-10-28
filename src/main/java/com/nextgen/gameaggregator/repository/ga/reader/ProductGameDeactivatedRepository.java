package com.nextgen.gameaggregator.repository.ga.reader;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nextgen.gameaggregator.entity.ga.ProductGameDeactivated;

@Repository
public interface ProductGameDeactivatedRepository extends JpaRepository<ProductGameDeactivated, Integer> {

    @Query(value = "SELECT * FROM product_game_deactivated " +
                   "WHERE (" +
                   "    (sas_entity_hierarchy_id = 1) OR " +
                   "    (sas_entity_hierarchy_id = 2 AND house_id = :houseId) OR " +
                   "    (sas_entity_hierarchy_id = 3 AND master_agent_id = :masterAgentId) OR " +
                   "    (sas_entity_hierarchy_id = 4 AND agent_id = :agentId)" +
                   ") " +
                   "AND product_game_id = :productGameId " +
                   "AND is_deleted = 0 " +
                   "LIMIT 1", nativeQuery = true)
    ProductGameDeactivated findProductGameDeactivated(Integer productGameId, Integer agentId, Integer masterAgentId, Integer houseId);
}
