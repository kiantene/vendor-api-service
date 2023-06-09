package com.nextgen.gameaggregator.vendor.alizegames.api.cancelbetnsettle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelBetNSettleDto implements RollbackData {
    private String traceId;
    private String betId;
    private String roundId;
    private String token;
    private String gameCode;
    private String username;
    private String operator;
    private Long timestamp;
    private String info;

    @Override
    public String getRollbackId() {
        return betId;
    }
    @Override
    public Long getVendorSettledTime() {
        return timestamp;
    }
}
