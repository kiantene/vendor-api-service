package com.nextgen.gameaggregator.entity;

import lombok.Data;
import javax.persistence.*;

@Entity
@Table(name = "vendor_raw_bet_data")
@Data
public class VendorRawBetData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer vendorId;
    private String data;
    private Long createDate;
}
