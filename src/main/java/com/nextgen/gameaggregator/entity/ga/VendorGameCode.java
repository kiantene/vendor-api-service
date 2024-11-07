package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "vendor_game_codes")
@Data
public class VendorGameCode extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private Integer productId;
    private Integer productGameId;
    private String openGameCode;
    private String betGameCode;
    private String imageSquare;
    private String imageLandscape;
    private Integer languageId;
    private Integer platformId;
    private Integer status;

    @ManyToOne
    private VendorGame vendorGame;
    private Integer vendorId;
}
