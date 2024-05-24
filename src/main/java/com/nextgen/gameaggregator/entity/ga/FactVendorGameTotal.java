package com.nextgen.gameaggregator.entity.ga;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "fact_vendor_game_total")
@Data
public class FactVendorGameTotal {
	
    @Id
    private String id;
    @Column(name = "day")
    private Long day;
    @Column(name = "vendor_game_id")
    private Integer vendorGameId;
    @Column(name = "vendor_id")
    private Integer vendorId;
    @Column(name = "game_category_id")
    private Integer gameCategoryId;
    @Column(name = "currency_id")
    private Integer currencyId;
    @Column(name = "total_bet_count")
    private BigDecimal totalBetCount;
    @Column(name = "total_bet_amount")
    private BigDecimal totalBetAmount;
    @Column(name = "total_win_amount")
    private BigDecimal totalWinAmount;
    @Column(name = "total_win_loss")
    private BigDecimal totalWinLoss;
    @Column(name = "total_effective_turnover")
    private BigDecimal totalEffectiveTurnover;
    @Column(name = "total_jackpot_amount")
    private BigDecimal totalJackpotAmount;
    @Column(name = "ggr")
    private BigDecimal ggr;
    @Column(name = "processed_date")
    private Long processedDate;
    
}
