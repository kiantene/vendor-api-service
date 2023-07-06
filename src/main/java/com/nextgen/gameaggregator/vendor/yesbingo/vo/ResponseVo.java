package com.nextgen.gameaggregator.vendor.yesbingo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.ResponseCodes;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ResponseVo implements HttpResponse {

    private String status;
    private BigDecimal balance;
    private String errText;

    @Override
    public boolean hasError() {
        return false;
    }

    public void setStatus(String responseCode) {
        this.status = responseCode;
        this.errText = null;
        if(!this.status.equals(ResponseCodes.SUCCEED)) {
            this.errText = ResponseCodes.RESPONSE_DESCRIPTION.get(this.status);
        }
    }

}



