package com.nextgen.gameaggregator.vendor.kypoker.api.cancel;

import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CancelDto implements RollbackData {

    private Integer s;

    private String account;

    private String orderId;

    private String gameNo;

    private Integer gameId;

    private Integer kindId;

    private BigDecimal money;

    private String currency;


    @Override
    public String getRollbackId() {
        return this.orderId;
    }

    @Override
    public Long getVendorSettledTime() {
        return 0L;
    }

    @Override
    public String getRoundId() {
        return this.gameNo;
    }
}
