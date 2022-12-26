package com.nextgen.gameaggregator.entity;

import javax.persistence.*;
import lombok.Data;

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
    private Integer status;
}
