package com.nextgen.gameaggregator.vendor.amusnet.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.amusnet.constant.ResponseCodes;
import lombok.Data;

import java.math.BigInteger;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {
    @JacksonXmlProperty(localName = "Balance")
    private BigInteger balance;
    @JacksonXmlProperty(localName = "CasinoTransferId")
    private String casinoTransferId;
    @JacksonXmlProperty(localName = "ErrorCode")
    private Integer errorCode;
    @JacksonXmlProperty(localName = "ErrorMessage")
    private String errorMessage;
    @JsonIgnore
    private String responseXMLFormat;

    public void setResponseCodes(ResponseCodes responseCodes) {
        this.errorCode = responseCodes.errorCode;
        this.errorMessage = responseCodes.errorMessage;
    }

    @Override
    public boolean hasError() {
        return !errorCode.equals(ResponseCodes.OK.errorCode);
    }

}
