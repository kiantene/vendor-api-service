package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RoundPayoutDto {

    public String userId;
    public String gameId;
    public String currency;
    public String roundId;
    public String gameType;
    public List<RoundPayoutTransactionDto> transactionList;
    public String userToken;
    public BigDecimal validTurnover;

    public static RoundPayoutTransactionDto findTransaction(List<RoundPayoutTransactionDto> list, String key) {
        for (RoundPayoutTransactionDto obj : list) {
            String index = obj.getType();
            if(index.equals(key)) {
                return obj;
            }
        }
        return null;
    }

    /*
    @JsonIgnore
    public String getExternalTransactionId() {
        return null;
    }

    @JsonIgnore
    public BigDecimal getAmount() {
        return null;
    }

    @JsonIgnore
    public Long getTimestamp() {
        return null;
    }

    public List<Long> getTimestamps(RoundPayoutDto roundPayoutDto) {
        List<RoundPayoutTransactionDto> transactionList = roundPayoutDto.transactionList;

        List<Long> timestamps = new ArrayList<>();
        for (RoundPayoutTransactionDto transaction : transactionList) {
            timestamps.add(transaction.getTimestamp());
        }

        return timestamps;
    }
     */
}
