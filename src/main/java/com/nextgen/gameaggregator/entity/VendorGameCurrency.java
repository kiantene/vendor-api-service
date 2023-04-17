package com.nextgen.gameaggregator.entity;

import lombok.Data;

import jakarta.persistence.*;

@Entity
@Table(name = "vendor_game_currencies")
@Data
public class VendorGameCurrency  extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer status;
    @ManyToOne
    private Currency currency;
    @ManyToOne
    private VendorGame vendorGame;

}
