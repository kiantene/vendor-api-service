package com.nextgen.gameaggregator.vendor.facai.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.operator.wallet.settled.UnsettledResultSettledData;
import lombok.Data;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto implements UnsettledResultSettledData {

    private String externalTransactionId;
    private String vendorBetId;
    private String roundId;
    private String gameId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal vendorWinLoss;
    private WinType resultType;
    private String rawVendorBetTime;
    private String rawResultTime;

    @Override
    public BigDecimal getWinLoss() {
        return (this.winAmount.subtract(this.betAmount));
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.betAmount;
    }

    @Override
    public BigDecimal getRefundAmount() {
        return BigDecimal.valueOf(0);
    }

    @Override
    public Long getVendorBetTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC-4"));
        try {
            Date date = dateFormat.parse(this.getRawVendorBetTime());
            return date.getTime();
        }catch (Exception exception) {
        }
        return Long.valueOf(000000000000);
    }

    @Override
    public Long getResultTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC-4"));
        try {
            Date date = dateFormat.parse(this.getRawResultTime());
            return date.getTime();
        }catch (Exception exception) {
        }
        return Long.valueOf(000000000000);
    }

    @Override
    public Long getVendorSettleTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC-4"));
        try {
            Date date = dateFormat.parse(this.getRawResultTime());
            return date.getTime();
        }catch (Exception exception) {
        }
        return Long.valueOf(000000000000);
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsCancelled() {
        return 0;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }
}