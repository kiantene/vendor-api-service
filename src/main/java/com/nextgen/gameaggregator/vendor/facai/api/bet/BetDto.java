package com.nextgen.gameaggregator.vendor.facai.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import lombok.Data;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto implements BetData {
    private String externalTransactionId;
    private BigDecimal amount;
    private String roundId;
    private String gameCode;
    private String eventTime;

    @Override
    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    @Override
    public String getRoundId() {
        return roundId;
    }

    @Override
    public String getGameId() {
        return gameCode;
    }

    @Override
    public Long getTimestamp(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC-4"));
        try {
            Date date = dateFormat.parse(this.getEventTime());
            return date.getTime();
        }catch (Exception exception) {
        }
        return Long.valueOf(000000000000);
    }
}