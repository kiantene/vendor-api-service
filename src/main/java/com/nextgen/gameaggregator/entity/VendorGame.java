package com.nextgen.gameaggregator.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "vendor_games")
@Data
public class VendorGame {

    @Id
    private Integer id;
    private String code;
    private String vendorGameCode;
    private String name;
    private Integer gameCategoryId;
    private Integer vendorId;
//    private Integer isWeb;
//
//    @Column(name = "is_h5")
//    private Integer isH5;
    private Integer status;
}
