package com.nextgen.gameaggregator.vendor.saba.api.cancelbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.sport.refund.SportRefundData;
import com.nextgen.gameaggregator.vendor.saba.dto.GeneralDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelBetDto extends GeneralDto implements SportRefundData {
    private String operationId;
    private String refId;
    private String userId;
    private String updateTime;
    private List<CancelBetTransactionDto> txns;

    @Override
    public String getExternalTransactionId() {
        return this.operationId;
    }

    @Override
    public String getVendorBetId() {
        return this.refId; // use refId instead of txId because cancel will process before confirm bet and not update vendorBetId
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
