package com.nextgen.gameaggregator.vendor.winfinity.vo;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.winfinity.constant.ErrorCodes;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {

    private DataVo data;
    private ErrorVo error;

    public ResponseVo() {
        this.data = null;
        this.error = null;
    }

    public void setDataVo(String traceId, BigDecimal balance) {
        // Convert to microseconds
        long currentTimeMillis = System.currentTimeMillis();
        long currentTimeMicros = currentTimeMillis * 1000;

        this.data = new DataVo();
        this.data.setTransaction(traceId);
        this.data.setBalance(balance);
        this.data.setTimestamp(currentTimeMicros);
    }

    public void setErrorVo(ErrorCodes errorCodes) {
        this.error = new ErrorVo();
        this.error.setErrorCode(errorCodes);
    }

    @Override
    public boolean hasError() {
        return this.getError() != null && this.getError().getErrorCodes() != null;
    }
}
