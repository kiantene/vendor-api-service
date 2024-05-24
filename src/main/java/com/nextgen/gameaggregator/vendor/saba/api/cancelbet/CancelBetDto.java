package com.nextgen.gameaggregator.vendor.saba.api.cancelbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.sport.refund.SportRefundData;
import com.nextgen.gameaggregator.vendor.saba.dto.GeneralDto;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelBetDto extends GeneralDto implements SportRefundData {
    private String action;
    private String operationId;
    private String userId;
    private String updateTime;
    private List<CancelBetTransactionDto> txns;

    private String refId;


    @Override
    public String getExternalTransactionId() {
        return this.operationId;
    }

    @Override
    public String getRoundId() {
        return this.refId;
    }

    @Override
    public String getVendorPlayerUsername() {
        return this.userId;
    }

    @Override
    public Long getTimestamp() {
        return System.currentTimeMillis();
    }
}
