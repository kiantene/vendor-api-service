package com.nextgen.gameaggregator.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "vendor_game_codes")
@Data
public class VendorGameCode  extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
//    private Integer vendorGameId;

//    private Integer vendorId;
    private String name;
    private String openGameCode;
    private String betGameCode;
    private String imageSquare;
    private String imageLandscape;
    private Integer languageId;
    private Integer platformId;
    private Integer status;

    @ManyToOne
    private VendorGame vendorGame;
    @ManyToOne
    private Vendor vendor;
}
