package com.nextgen.gameaggregator.vendor.jdb.api.promo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PromoPayoutRequest {
    private String x;
    private Integer action;
    private Long ts;
    private Long transferId;
    private String eventId;
    private String uid;
    private BigDecimal amount;
    private String currency;
    private BigDecimal accumulatedTurnover;
    private BigDecimal accumulatedWin;
    private String cashoutTime;
}
