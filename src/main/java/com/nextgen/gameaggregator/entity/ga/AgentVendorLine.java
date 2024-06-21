package com.nextgen.gameaggregator.entity.ga;

import lombok.Data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "agent_vendor_lines")
@Data
public class AgentVendorLine {
    @Id
    private Integer id;
    private Integer agentId;
    private Integer vendorLineId;
    private Integer currencyId;
    private Integer gameCategoryId;
    private Integer vendorId;
    private Integer status;
}
