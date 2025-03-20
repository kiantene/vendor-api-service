package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "product_games")
public class ProductGame {

    @Id
    private Integer id;
    private Integer productId;
    private Integer gameCategoryId;
    private String code;
    private String name;
    private Integer isPremiumGame;
    private Boolean requireDebit;
    private Double defaultCharges;
    private String vendorGameCode;
    private Integer vendorGameId;
    private Integer vendorId;
    private Integer status;
    private Long createById;
    private String createByUsertype;
    private String createByIp;
    private Long createDate;

    public boolean isLaunchByProductGame() {
        return this.createById == 10;
    }
}
