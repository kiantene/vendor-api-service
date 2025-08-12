package com.nextgen.gameaggregator.entity.ga;

import lombok.Data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "agent_vendor_proxies")
@Data
public class AgentVendorProxy {
    @Id
    private Integer id;
    private Integer agentId;
    private Integer vendorId;
    private String vendorDomain;
    private String proxyDomain;
    private Integer status;
}
