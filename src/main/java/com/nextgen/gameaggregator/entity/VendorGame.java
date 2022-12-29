package com.nextgen.gameaggregator.entity;

import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "vendor_games")
@Data
public class VendorGame {

    @Id
    private Integer id;
    private String code;
    private String name;
    private Integer gameCategoryId;
    private Integer vendorId;
    private Integer isWeb;

    @Column(name = "is_h5")
    private Integer isH5;
    private Integer status;
}
