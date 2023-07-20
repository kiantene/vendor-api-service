package com.nextgen.gameaggregator.vendor.playngo.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "reserve")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReserveVo implements HttpResponse {

    @JacksonXmlProperty(localName = "real")
    private String real;

    @JacksonXmlProperty(localName = "statusCode")
    private String statusCode;

    @JacksonXmlProperty(localName = "statusMessage")
    private String statusMessage;

    @Override
    public boolean hasError() {
        return false;
    }
}
