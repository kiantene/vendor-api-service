package com.nextgen.gameaggregator.repository.ga.reader;

import com.nextgen.gameaggregator.entity.ga.AgentProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentProductDetailRepository extends JpaRepository<AgentProductDetail, Integer> {
    @Query(value = """
            SELECT
                apd.*,
                pd.game_category_id,
                pd.currency_id
            FROM agent_product_details apd
            INNER JOIN product_details pd ON apd.product_detail_id = pd.id
            WHERE pd.product_id = :productId
            AND apd.agent_id = :agentId
            AND pd.status = 1
            AND apd.status = 1
            """, nativeQuery = true)
    List<AgentProductDetail> findVendorLine(Integer productId, Integer agentId);
}
