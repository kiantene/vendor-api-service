package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "vendor_payout_settings")
@Data
public class AgentPayoutSetting {
    @Id
    private String id;
    private Integer masterAgentId;
    private Integer agentId;
    private Integer vendorId;
    private Integer gameCategoryId;
    private Integer currencyId;
    private Integer version;
    private BigDecimal maxPayout;
    private Long createDate;
    private Integer status;
}
