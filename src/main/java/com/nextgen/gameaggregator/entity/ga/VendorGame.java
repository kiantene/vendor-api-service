package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "vendor_games")
@Data
public class VendorGame extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String code;
    private String vendorGameCode;
    private String name;

    private Integer betDataPreprocessing;

    private Integer gameCategoryId;
    private Integer vendorId;

    private Integer isByCurrency;
    private String imageSquare;
    private String imageLandscape;
    private Integer status;

    private Boolean requireDebit;
}
