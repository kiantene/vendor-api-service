package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "agent_product_details")
public class AgentProductDetail {
    @Id
    private Integer id;
    private Integer agentId;
    private Integer productDetailId;
    private Integer gameCategoryId;
    private Integer currencyId;
    private Integer vendorLineId;
    private Integer status;
}
