package com.nextgen.gameaggregator.entity.ga;

import lombok.Data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "agent_vendor_lines")
@Data
public class AgentVendorLine {
    @Id
    private Integer id;
    @ManyToOne
    private Agent agent;
    @ManyToOne
    private Vendor vendor;
    @ManyToOne
    private VendorLine vendorLine;
    @ManyToOne
    private Currency currency;
    @ManyToOne
    private GameCategory gameCategory;
    private Integer status;
}
