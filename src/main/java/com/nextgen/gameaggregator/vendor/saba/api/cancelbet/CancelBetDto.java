package com.nextgen.gameaggregator.vendor.saba.api.cancelbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.sport.entity.SportRefundData;
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
    private List<CancelBetTxnsDto> txns;

    private String refId;


    @Override
    public String getExternalTransactionId() {
        return this.refId;
    }

    @Override
    public String getVendorPlayerUsername() {
        return this.userId;
    }
}
