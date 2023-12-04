package com.nextgen.gameaggregator.vendor.advantplay.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.advantplay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.advantplay.service.VendorService;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ResponseVo implements HttpResponse {

    private String timestamp;
    private String seq;
    private Integer errorCode;
    private String errorDescription;
    @JsonProperty("OPTransId")
    private String opTransId;
    private BigDecimal balance;

    @JsonIgnore
    private ResponseCodes responseCodes;

    public ResponseVo() {
        this.setResponseCodes(ResponseCodes.SUCCESS);
    }

    public void setResponseCodes(ResponseCodes responseCodes) {
        this.responseCodes = responseCodes;
        this.errorCode = responseCodes.errorCode;
        this.errorDescription = responseCodes.errorDescription;
        this.timestamp = VendorService.getTimestamp();
    }

    @Override
    public boolean hasError() {
        return !this.responseCodes.equals(ResponseCodes.SUCCESS);
    }
}
