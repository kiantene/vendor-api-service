package com.nextgen.gameaggregator.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
