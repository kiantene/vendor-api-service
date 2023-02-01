package com.nextgen.gameaggregator.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "vendor_game_codes")
@Data
public class VendorGameCode {
    @Id
    private Integer id;
    private Integer vendorGameId;
    private String name;
    private String openGameCode;
    private String betGameCode;
    private Integer languageId;
    private Integer platformId;
    private Integer status;
}
