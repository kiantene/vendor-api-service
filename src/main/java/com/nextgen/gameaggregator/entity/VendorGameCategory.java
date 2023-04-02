package com.nextgen.gameaggregator.entity;


import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "vendor_game_categories")
@Data
public class VendorGameCategory {

    @Id
    private Integer id;
    private Integer vendorId;
    private Integer gameCategoryId;
}
