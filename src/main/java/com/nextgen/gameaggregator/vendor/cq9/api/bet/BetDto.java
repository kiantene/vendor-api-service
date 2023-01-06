package com.nextgen.gameaggregator.vendor.cq9.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import lombok.Data;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto implements BetData {
    private String account;
    private String eventTime;
    private String gamehall;
    private String gamecode;
    private String roundid;
    private BigDecimal amount;
    private String mtcode;
    private String session;
    private String platform;

    @Override
    public String getExternalTransactionId() {
        return mtcode;
    }

    @Override
    public String getRoundId() {
        return roundid;
    }

    @Override
    public String getGameId() {
        return gamecode;
    }

    @Override
    public Long getTimestamp() {
        Long timestamp;
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            Date date = simpleDateFormat.parse(this.getEventTime());
            timestamp = date.getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return timestamp;
    }
}
