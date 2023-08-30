package com.nextgen.gameaggregator.vendor.playngo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.playngo.constant.ResponseCodes;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo implements HttpResponse {
    @JacksonXmlProperty(localName = "real")
    private String real;
    @JacksonXmlProperty(localName = "statusCode")
    private Integer statusCode;
    @JacksonXmlProperty(localName = "statusMessage")
    private String statusMessage;
    private String ResponseXMLFormat;

    @Override
    public boolean hasError() {
        return !this.statusCode.equals(ResponseCodes.OK);
    }

    public void setStatusCode(Integer responseCode) {
        this.statusCode = responseCode;
        this.statusMessage = ResponseCodes.RESPONSE_DESCRIPTION.get(responseCode);
    }
}
