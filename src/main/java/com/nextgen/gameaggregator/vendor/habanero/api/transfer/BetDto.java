package com.nextgen.gameaggregator.vendor.habanero.api.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto implements BetResultData {

    private String externalTransactionId;

    private String vendorBetId;

    private String roundId;

    private String gameId;

    private BigDecimal betAmount;

    private BigDecimal winAmount;

    private BigDecimal winLoss;

    private BigDecimal effectiveTurnover;

    private String rawVendorBetTime;

    private String rawResultTime;

    private String rawVendorSettleTime;

    private BigDecimal jackpotAmount;

    private Integer isFreespin;

    private BetStatus betStatus;

    @Override
    public Long getVendorBetTime() {
        //convert date time string to timestamp
        if(this.getRawVendorBetTime() != null){
            LocalDateTime localDateTime = LocalDateTime.parse(this.getRawVendorBetTime(), DateTimeFormatter.ISO_DATE_TIME);
            ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC"));
            long timestamp = zonedDateTime.toInstant().toEpochMilli();
            return timestamp;
        } else {
            return null;
        }
    }

    @Override
    public Long getResultTime() {
        //convert date time string to timestamp
        if(this.getRawResultTime() != null){
            LocalDateTime localDateTime = LocalDateTime.parse(this.getRawResultTime(), DateTimeFormatter.ISO_DATE_TIME);
            ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC"));
            long timestamp = zonedDateTime.toInstant().toEpochMilli();
            return timestamp;
        } else {
            return null;
        }
    }

    @Override
    public Long getVendorSettleTime() {
        //convert date time string to timestamp
        if(this.getRawVendorSettleTime() != null){
            LocalDateTime localDateTime = LocalDateTime.parse(this.getRawVendorSettleTime(), DateTimeFormatter.ISO_DATE_TIME);
            ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC"));
            long timestamp = zonedDateTime.toInstant().toEpochMilli();
            return timestamp;
        } else {
            return null;
        }
    }
}
