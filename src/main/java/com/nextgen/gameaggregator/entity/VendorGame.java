package com.nextgen.gameaggregator.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "vendor_games")
@Data
public class VendorGame extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String code;
    private String vendorGameCode;
    private String name;
    //    private Integer gameCategoryId;
    @ManyToOne
    private GameCategory gameCategory;

    //    private Integer vendorId;
    @ManyToOne
    private Vendor vendor;

    private Integer isByCurrency;
    private String imageSquare;
    private String imageLandscape;
    private Integer status;
}
