package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "vendor_campaign_games")
@Data
public class VendorCampaignGame {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "vendor_game_code")
    private String vendorGameCode;

    @Column(name = "code")
    private String code;

    @Column(name = "is_support_free_round")
    private Integer isSupportFreeRound;
}
