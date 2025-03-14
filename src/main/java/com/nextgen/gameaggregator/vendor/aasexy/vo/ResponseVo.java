package com.nextgen.gameaggregator.vendor.aasexy.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.aasexy.constant.ResponseCodes;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {
    private BigDecimal balance;
    private String status;
    private String desc;
    @JsonIgnore
    private Integer httpStatus;
    @JsonIgnore
    private ResponseCodes responseCodes;

    public ResponseVo() {
        this.setResponseCodes(ResponseCodes.SUCCESS);
    }

    public void setResponseCodes(ResponseCodes responseCodes) {
        this.responseCodes = responseCodes;
        this.status = responseCodes.status;
        this.desc = responseCodes.desc;
    }

    @Override
    public boolean hasError() {
        return !this.responseCodes.equals(ResponseCodes.SUCCESS);
    }

}
